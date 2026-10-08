import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Article, ShoppingListType } from '../models/article.model';

@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/articles';

  getArticles(listType: ShoppingListType): Observable<Article[]> {
    return this.http.get<Article[]>(this.apiUrl, { params: { list: listType } });
  }
}