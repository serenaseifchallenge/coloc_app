import { Component, input, output } from '@angular/core';
import { Article } from '../../models/article.model';

@Component({
  selector: 'app-article-item',
  templateUrl: './article-item.html',
  styleUrl: './article-item.css',
  host: { '[class.buying]': 'buying()' },
})
export class ArticleItem {
  readonly article = input.required<Article>();
  readonly buying = input(false);
  readonly buyRequested = output<Article>();
}