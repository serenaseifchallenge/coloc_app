import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Article, CreateArticlesRequest, ShoppingListType, UpdateArticleRequest } from '../models/article.model';

@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/articles';

  getArticles(listType: ShoppingListType): Observable<Article[]> {
    return this.http.get<Article[]>(this.apiUrl, { params: { list: listType } });
  }

  createArticles(listType: ShoppingListType, names: string[]): Observable<Article[]> {
    const request: CreateArticlesRequest = { list: listType, names };
    return this.http.post<Article[]>(this.apiUrl, request);
  }

  updateArticle(articleId: number, name: string): Observable<Article> {
    const request: UpdateArticleRequest = { name };
    return this.http.patch<Article>(`${this.apiUrl}/${articleId}`, request);
  }

  buyArticle(articleId: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${articleId}/buy`, null);
  }
}