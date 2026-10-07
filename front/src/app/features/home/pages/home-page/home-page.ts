import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedHouseService } from '../../../../core/shared-house/shared-house.service';
import { titleFor } from '../../../../shared/utils/titles';
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

  protected readonly today = new Date();
  protected readonly user = this.auth.user;
  protected readonly house = this.houses.house;

  protected readonly members = computed(() =>
    (this.house()?.members ?? []).map((m) => ({ ...m, title: titleFor(m.points).toLowerCase() })),
  );

  /**
   * Mes tâches pas encore faites (undefined = chargement, null = erreur).
   * toSignal s'abonne et se désabonne tout seul quand on quitte la page.
   */
  private readonly myTasks = toSignal(
    this.taskService.getTasks({ done: false, assignedToMe: true }).pipe(catchError(() => of(null))),
  );

  private readonly taskLines = computed<string[]>(() => {
    const tasks = this.myTasks();
    if (tasks === undefined) return ['…'];
    if (tasks === null) return ['Tâches indisponibles'];
    const dueToday = tasks.filter((task) => task.deadline === todayIso()).length;
    return [count(tasks.length, 'tâche'), `${count(dueToday, 'tâche')} aujourd’hui`];
  });

  /**
   * TODO équipe : remplacer les « — » par les vraies valeurs quand vos API seront prêtes
   * (même principe que les tâches ci-dessus).
   * L'ordre compte : la grille se remplit ligne par ligne (Tâches, Dépenses, Courses, ...).
   */
  protected readonly tiles = computed<HomeTile[]>(() => {
    const me = this.user();
    return [
      { title: 'Tâches', route: '/tasks', color: 'salmon', lines: this.taskLines() },
      { title: 'Dépenses', route: '/expenses', color: 'purple', lines: ['— € dû'] },
      { title: 'Courses', route: '/shopping', color: 'purple', lines: [`— pour ${this.house()?.name ?? 'la coloc'}`, '— pour vous'] },
      { title: 'Évènements', route: '/calendar', color: 'salmon', lines: ['— ce mois-ci', '— aujourd’hui'] },
      { title: 'Notes', route: '/notes', color: 'salmon', lines: ['— affichées', '— vote en cours'] },
      {
        title: 'Points',
        route: '/points',
        color: 'purple',
        lines: [`-- ${titleFor(me?.points)}`, `${me?.points ?? 0} pts ce mois-ci`],
      },
    ];
  });
}
