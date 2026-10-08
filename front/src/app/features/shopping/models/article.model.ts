export type ShoppingListType = 'PERSONAL' | 'SHARED';

export interface Article {
  id: number;
  name: string;
}

export interface CreateArticlesRequest {
  list: ShoppingListType;
  names: string[];
}

export interface UpdateArticleRequest {
  name: string;
}