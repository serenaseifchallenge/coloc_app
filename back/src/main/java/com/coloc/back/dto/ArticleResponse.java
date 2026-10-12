package com.coloc.back.dto;

import com.coloc.back.entity.Article;

public record ArticleResponse(Long id, String name, boolean bought) {

    public static ArticleResponse from(Article article) {
        return new ArticleResponse(article.getId(), article.getName(), article.isBought());
    }
}