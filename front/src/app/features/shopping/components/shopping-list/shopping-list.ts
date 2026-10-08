import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AutoFocus } from '../../../../shared/directives/auto-focus';
import { Article, ShoppingListType } from '../../models/article.model';
import { ArticleService } from '../../services/article.service';
import { ArticleItem } from '../article-item/article-item';

interface ArticleDraft {
  id: number;
  name: string;
}

@Component({
  selector: 'app-shopping-list',
  imports: [ArticleItem, AutoFocus],
  templateUrl: './shopping-list.html',
  styleUrl: './shopping-list.css',
  host: { '[class]': 'theme()' },
})
export class ShoppingList implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly destroyRef = inject(DestroyRef);
  private nextDraftId = 0;

  readonly listType = input.required<ShoppingListType>();
  readonly heading = input.required<string>();
  readonly description = input('');

  readonly theme = computed(() => (this.listType() === 'PERSONAL' ? 'personal' : 'shared'));
  readonly articles = signal<Article[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly adding = signal(false);
  readonly drafts = signal<ArticleDraft[]>([]);
  readonly saving = signal(false);
  readonly buyingIds = signal<ReadonlySet<number>>(new Set());

  ngOnInit(): void {
    this.loadArticles();
  }

  onFooterButtonClick(): void {
    if (this.adding()) {
      this.saveDrafts();
    } else {
      this.startAdding();
    }
  }

  updateDraft(draftId: number, name: string): void {
    this.drafts.update((drafts) => drafts.map((draft) => (draft.id === draftId ? { ...draft, name } : draft)));
  }

  onDraftEnter(draftId: number): void {
    const drafts = this.drafts();
    const draft = drafts.find((current) => current.id === draftId);
    const isLastDraft = drafts.at(-1)?.id === draftId;

    if (draft && draft.name.trim() && isLastDraft) {
      this.drafts.update((current) => [...current, this.createDraft()]);
    }
  }

  cancelAdding(): void {
    this.adding.set(false);
    this.drafts.set([]);
    this.actionError.set(null);
  }

  buyArticle(article: Article): void {
    this.setBuying(article.id, true);
    this.actionError.set(null);

    this.articleService
      .buyArticle(article.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.articles.update((articles) => articles.filter((current) => current.id !== article.id));
          this.setBuying(article.id, false);
        },
        error: () => {
          this.setBuying(article.id, false);
          this.actionError.set(`« ${article.name} » n'a pas pu être coché. Réessaie.`);
        },
      });
  }

  private setBuying(articleId: number, buying: boolean): void {
    this.buyingIds.update((ids) => {
      const nextIds = new Set(ids);
      if (buying) {
        nextIds.add(articleId);
      } else {
        nextIds.delete(articleId);
      }
      return nextIds;
    });
  }

  private startAdding(): void {
    this.actionError.set(null);
    this.drafts.set([this.createDraft()]);
    this.adding.set(true);
  }

  private saveDrafts(): void {
    const names = this.drafts()
      .map((draft) => draft.name.trim())
      .filter((name) => name.length > 0);

    if (names.length === 0) {
      this.cancelAdding();
      return;
    }

    this.saving.set(true);
    this.actionError.set(null);

    this.articleService
      .createArticles(this.listType(), names)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (createdArticles) => {
          this.articles.update((articles) => [...articles, ...createdArticles]);
          this.saving.set(false);
          this.cancelAdding();
        },
        error: () => {
          this.actionError.set("Les articles n'ont pas pu être ajoutés. Réessaie.");
          this.saving.set(false);
        },
      });
  }

  private createDraft(): ArticleDraft {
    return { id: this.nextDraftId++, name: '' };
  }

  private loadArticles(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.articleService
      .getArticles(this.listType())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (articles) => {
          this.articles.set(articles);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Impossible de charger la liste.');
          this.loading.set(false);
        },
      });
  }
}