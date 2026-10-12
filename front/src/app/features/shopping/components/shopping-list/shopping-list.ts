import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  WritableSignal,
  inject,
  input,
  afterNextRender,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AutoFocus } from '../../../../shared/directives/auto-focus';
import { Article, ShoppingListType } from '../../models/article.model';
import { ArticleService } from '../../services/article.service';
import { ArticleItem } from '../article-item/article-item';

@Component({
  selector: 'app-shopping-list',
  imports: [ArticleItem, AutoFocus],
  templateUrl: './shopping-list.html',
  styleUrl: './shopping-list.css',
})
export class ShoppingList implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  readonly listType = input.required<ShoppingListType>();
  readonly heading = input.required<string>();

  private readonly draftInput = viewChild<ElementRef<HTMLInputElement>>('draftInput');

  readonly articles = signal<Article[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly adding = signal(false);
  readonly draftName = signal('');

  readonly togglingIds = signal<ReadonlySet<number>>(new Set());
  readonly deletingIds = signal<ReadonlySet<number>>(new Set());
  readonly editingId = signal<number | null>(null);
  readonly renamingId = signal<number | null>(null);

  ngOnInit(): void {

    this.loadArticles();
  }

  onFooterButtonClick(): void {
    if (this.adding()) {
      this.finishAdding();
    } else {
      this.startAdding();
    }
  }


  onDraftEnter(event: Event): void {
    event.preventDefault();

    const name = this.draftName().trim();
    if (!name) {
      this.cancelAdding();
      return;
    }

    this.draftName.set('');
    this.saveArticle(name);
    this.focusDraftInput();
  }

  finishAdding(): void {
    const name = this.draftName().trim();
    if (name) {
      this.saveArticle(name);
    }
    this.cancelAdding();
  }

  cancelAdding(): void {
    this.adding.set(false);
    this.draftName.set('');
  }


  startEditing(id: number): void {
    this.cancelAdding();
    this.editingId.set(id);
  }

  cancelEditing(): void {
    this.editingId.set(null);
  }

  renameArticle(article: Article, newName: string): void {
    const name = newName.trim();
    if (!name || name === article.name) {
      this.cancelEditing();
      return;
    }

    this.actionError.set(null);
    this.renamingId.set(article.id);

    this.articleService
      .updateArticle(article.id, name)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.replaceArticle(updated);
          this.renamingId.set(null);
          this.editingId.set(null);
        },
        error: (error: HttpErrorResponse) => {
          this.renamingId.set(null);
          this.editingId.set(null);
          if (error.status === 409) {
            this.replaceArticle({ ...article, bought: true });
            this.actionError.set('Cet article a déjà été acheté, il ne peut plus être modifié.');
          } else if (error.status === 404) {
            this.removeArticle(article.id);
            this.actionError.set("Cet article n'existe plus.");
          } else {
            this.actionError.set("Impossible de renommer l'article.");
          }
        },
      });
  }


  toggleBought(article: Article): void {
    if (this.togglingIds().has(article.id)) return;

    this.actionError.set(null);
    this.toggleId(this.togglingIds, article.id, true);

    const request$ = article.bought
      ? this.articleService.unbuyArticle(article.id)
      : this.articleService.buyArticle(article.id);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.replaceArticle({ ...article, bought: !article.bought });
        this.toggleId(this.togglingIds, article.id, false);
      },
      error: () => {
        this.toggleId(this.togglingIds, article.id, false);
        this.actionError.set("Impossible de mettre à jour l'article.");
      },
    });
  }

  deleteArticle(article: Article): void {
    if (this.deletingIds().has(article.id)) return;

    this.actionError.set(null);
    this.toggleId(this.deletingIds, article.id, true);

    this.articleService
      .deleteArticle(article.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.removeArticle(article.id);
          this.toggleId(this.deletingIds, article.id, false);
        },
        error: () => {
          this.toggleId(this.deletingIds, article.id, false);
          this.actionError.set("Impossible de supprimer l'article.");
        },
      });
  }


  private startAdding(): void {
    this.actionError.set(null);
    this.editingId.set(null);
    this.draftName.set('');
    this.adding.set(true);
  }

  private saveArticle(name: string): void {
    this.actionError.set(null);

    this.articleService
      .createArticles(this.listType(), [name])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (created) => {
          this.articles.update((list) => [...list, ...created]);
          this.scrollToDraft();
        },
        error: () => {
          this.actionError.set(`Impossible d'ajouter « ${name} ».`);
          if (this.adding() && this.draftName() === '') {
            this.draftName.set(name);
          }
        },
      });
  }

  private focusDraftInput(): void {
    this.draftInput()?.nativeElement.focus();
    this.scrollToDraft();
  }

  private scrollToDraft(): void {
    afterNextRender(
      () => this.draftInput()?.nativeElement.scrollIntoView({ block: 'nearest', behavior: 'smooth' }),
      { injector: this.injector },
    );
  }

  private replaceArticle(updated: Article): void {
    this.articles.update((list) => list.map((a) => (a.id === updated.id ? updated : a)));
  }

  private removeArticle(id: number): void {
    this.articles.update((list) => list.filter((a) => a.id !== id));
  }

  /** Ajoute/retire un id d'un Set stocké dans un signal, sans muter l'ancien Set. */
  private toggleId(ids: WritableSignal<ReadonlySet<number>>, id: number, present: boolean): void {
    ids.update((current) => {
      const next = new Set(current);
      if (present) {
        next.add(id);
      } else {
        next.delete(id);
      }
      return next;
    });
  }

  private loadArticles(): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.articleService
      .getArticles(this.listType())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (articles) => {
          this.articles.set(articles);
          this.loading.set(false);
        },
        error: () => {
          this.loadError.set('Impossible de charger la liste.');
          this.loading.set(false);
        },
      });
  }
}