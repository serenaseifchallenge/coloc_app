import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, filter, of, switchMap } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedHouseService } from '../../../../core/shared-house/shared-house.service';
import { titleFor } from '../../../../shared/utils/titles';
import { NotesService } from '../../../../notes/notes.service';
import { CalendarService } from '../../../../calendar/calendar.service';
import { ExpenseService } from '../../../expenses/services/expense.service';
import { PotService } from '../../../expenses/services/pot.service';
import { EurPipe } from '../../../expenses/utils/eur.pipe';
import { monthIso } from '../../../expenses/utils/money';
import { ArticleService } from '../../../shopping/services/article.service';
import { TaskService } from '../../../tasks/services/task.service';
import { todayIso } from '../../../tasks/utils/task-status';

interface HomeTile {
  title: string;
  route: string;
  color: 'salmon' | 'purple';
  lines: string[];
}

/** « 0 tâche », « 1 tâche », « 2 tâches » (en français, 0 reste au singulier). */
function count(n: number, word: string): string {
  return `${n} ${word}${n > 1 ? 's' : ''}`;
}

/** Montant arrondi au centime, pour éviter les 0,1 + 0,2 = 0,30000000004. */
function cents(amount: number): number {
  return Math.round(amount * 100) / 100;
}

@Component({
  selector: 'app-home-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage {
  private readonly auth = inject(AuthService);
  private readonly houses = inject(SharedHouseService);
  private readonly taskService = inject(TaskService);
  private readonly articleService = inject(ArticleService);
  private readonly expenseService = inject(ExpenseService);
  private readonly potService = inject(PotService);
  private readonly notesService = inject(NotesService);
  private readonly calendarService = inject(CalendarService);
  private readonly eur = new EurPipe();

  protected readonly today = new Date();
  protected readonly user = this.auth.user;
  protected readonly house = this.houses.house;

  protected readonly members = computed(() =>
    (this.house()?.members ?? []).map((m) => ({ ...m, title: titleFor(m.points) })),
  );

  constructor() {
    // Les points changent quand on coche des tâches : on les relit à chaque arrivée sur l'accueil.
    this.auth.refreshUser().pipe(takeUntilDestroyed()).subscribe();
    this.houses.load(true).catch(() => {
      /* on garde les anciennes infos si le back ne répond pas */
    });
  }

  // Pour chaque cadre : undefined = chargement, null = erreur.
  // toSignal s'abonne et se désabonne tout seul quand on quitte la page.

  // ---------- Tâches ----------
  private readonly myTasks = toSignal(
    this.taskService.getTasks({ done: false, assignedToMe: true }).pipe(catchError(() => of(null))),
  );

  private readonly taskLines = computed<string[]>(() => {
    const tasks = this.myTasks();
    if (tasks === undefined) return ['…'];
    if (tasks === null) return ['Tâches indisponibles'];
    const today = todayIso();
    const dueToday = tasks.filter((task) => task.deadline === today).length;
    const late = tasks.filter((task) => task.deadline !== null && task.deadline < today).length;
    const lines = [count(tasks.length, 'tâche'), `${count(dueToday, 'tâche')} aujourd’hui`];
    if (late > 0) {
      lines.push(`${late} en retard`);
    }
    return lines;
  });

  // ---------- Dépenses et cagnottes ----------
  private readonly expenseSummary = toSignal(
    this.expenseService.getSummary(monthIso(new Date())).pipe(catchError(() => of(null))),
  );
  private readonly pots = toSignal(this.potService.getPots().pipe(catchError(() => of(null))));

  private readonly expenseLines = computed<string[]>(() => {
    const summary = this.expenseSummary();
    if (summary === undefined) return ['…'];
    if (summary === null) return ['Dépenses indisponibles'];

    // Solde : positif = on me doit de l'argent, négatif = je dois de l'argent.
    const balance = cents(summary.myBalance);
    const lines =
      balance < 0 ? [`Vous devez ${this.eur.transform(-balance, 'short')}`]
      : balance > 0 ? [`On vous doit ${this.eur.transform(balance, 'short')}`]
      : ['Vous êtes à jour'];

    // Cagnottes : ce qu'il me reste à verser dans celles où je participe.
    const toPay = cents(
      (this.pots() ?? [])
        .filter((pot) => pot.participant && pot.myShare !== null && pot.remaining > 0)
        .reduce((sum, pot) => sum + Math.max(0, (pot.myShare ?? 0) - pot.myPaid), 0),
    );
    if (toPay > 0) {
      lines.push(`${this.eur.transform(toPay, 'short')} à verser en cagnotte`);
    }
    return lines;
  });

  // ---------- Courses ----------
  private readonly sharedArticles = toSignal(
    this.articleService.getArticles('SHARED').pipe(catchError(() => of(null))),
  );
  private readonly personalArticles = toSignal(
    this.articleService.getArticles('PERSONAL').pipe(catchError(() => of(null))),
  );

  private readonly shoppingLines = computed<string[]>(() => {
    const shared = this.sharedArticles();
    const personal = this.personalArticles();
    if (shared === undefined || personal === undefined) return ['…'];
    if (shared === null || personal === null) return ['Courses indisponibles'];
    return [
      `${count(shared.filter((article) => !article.bought).length, 'article')} pour ${this.house()?.name ?? 'la coloc'}`,
      `${count(personal.filter((article) => !article.bought).length, 'article')} pour vous`,
    ];
  });

  // ---------- Évènements ----------
  private readonly events = toSignal(this.calendarService.getEvents().pipe(catchError(() => of(null))));

  private readonly eventLines = computed<string[]>(() => {
    const events = this.events();
    if (events === undefined) return ['…'];
    if (events === null) return ['Évènements indisponibles'];

    // Les dates sont au format « 2026-10-11 » : on peut les comparer comme du texte.
    const today = todayIso();
    const month = today.slice(0, 7); // « 2026-10 »
    const monthStart = `${month}-01`;
    const monthEnd = `${month}-31`; // suffit pour comparer, même pour les mois plus courts

    // Un évènement compte s'il chevauche la période (ex. un week-end du 31 au 2).
    const thisMonth = events.filter((e) => e.startDate <= monthEnd && e.endDate >= monthStart).length;
    const todayCount = events.filter((e) => e.startDate <= today && e.endDate >= today).length;
    return [`${thisMonth} ce mois-ci`, `${todayCount} aujourd’hui`];
  });

  // ---------- Notes ----------
  // Son service a besoin de l'id de la coloc et de l'utilisateur : on attend que les deux soient connus.
  private readonly noteIds = computed(() => {
    const houseId = this.house()?.id;
    const userId = this.user()?.id;
    return houseId && userId ? { houseId, userId } : null;
  });

  private readonly notes = toSignal(
    toObservable(this.noteIds).pipe(
      filter((ids) => ids !== null),
      switchMap((ids) => this.notesService.getNotes().pipe(catchError(() => of(null)))),
    ),
  );

  private readonly noteLines = computed<string[]>(() => {
    const notes = this.notes();
    if (notes === undefined) return ['…'];
    if (notes === null) return ['Notes indisponibles'];
    const announcements = notes.filter((note) => note.type === 'ANNONCE').length;
    const pendingPolls = notes.filter(
      (note) => note.type === 'SONDAGE' && !note.options.some((option) => option.votedByCurrentUser),
    ).length;
    return [count(announcements, 'annonce'), `${count(pendingPolls, 'sondage')} à voter`];
  });

  /** L'ordre compte : la grille se remplit ligne par ligne (Tâches, Dépenses, Courses, ...). */
  protected readonly tiles = computed<HomeTile[]>(() => {
    const me = this.user();
    return [
      { title: 'Tâches', route: '/tasks', color: 'salmon', lines: this.taskLines() },
      { title: 'Dépenses', route: '/expenses', color: 'purple', lines: this.expenseLines() },
      { title: 'Courses', route: '/shopping', color: 'purple', lines: this.shoppingLines() },
      { title: 'Évènements', route: '/calendar', color: 'salmon', lines: this.eventLines() },
      { title: 'Notes', route: '/notes', color: 'salmon', lines: this.noteLines() },
      {
        title: 'Points',
        route: '/points',
        color: 'purple',
        lines: [`${titleFor(me?.points)}`, `${me?.points ?? 0} pts ce mois-ci`],
      },
    ];
  });
}