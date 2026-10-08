import { Component, input } from '@angular/core';
import { Article } from '../../models/article.model';

@Component({
  selector: 'app-article-item',
  templateUrl: './article-item.html',
  styleUrl: './article-item.css',
})
export class ArticleItem {
  readonly article = input.required<Article>();
}