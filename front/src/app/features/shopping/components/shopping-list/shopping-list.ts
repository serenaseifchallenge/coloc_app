import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Article, ShoppingListType } from '../../models/article.model';
import { ArticleService } from '../../services/article.service';
import { ArticleItem } from '../article-item/article-item';

@Component({
  selector: 'app-shopping-list',
  imports: [ArticleItem],
  templateUrl: './shopping-list.html',
  styleUrl: './shopping-list.css',
  host: { '[class]': 'theme()' },
})
export class ShoppingList implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly destroyRef = inject(DestroyRef);

  readonly listType = input.required<ShoppingListType>();
  readonly heading = input.required<string>();
  readonly description = input('');

  readonly theme = computed(() => (this.listType() === 'PERSONAL' ? 'personal' : 'shared'));
  readonly articles = signal<Article[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadArticles();
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