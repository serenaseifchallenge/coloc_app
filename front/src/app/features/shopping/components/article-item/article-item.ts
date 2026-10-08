import { Component, input, output } from '@angular/core';
import { AutoFocus } from '../../../../shared/directives/auto-focus';
import { Article } from '../../models/article.model';

@Component({
  selector: 'app-article-item',
  imports: [AutoFocus],
  templateUrl: './article-item.html',
  styleUrl: './article-item.css',
  host: {
    '[class.buying]': 'buying()',
    '[class.deleting]': 'deleting()',
  },
})
export class ArticleItem {
  readonly article = input.required<Article>();
  readonly buying = input(false);
  readonly deleting = input(false);
  readonly editing = input(false);
  readonly saving = input(false);

  readonly buyRequested = output<Article>();
  readonly deleteRequested = output<Article>();
  readonly editRequested = output<Article>();
  readonly renameRequested = output<string>();
  readonly editCancelled = output<void>();
}