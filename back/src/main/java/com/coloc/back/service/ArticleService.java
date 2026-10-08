package com.coloc.back.service;

import com.coloc.back.dto.ArticleResponse;
import com.coloc.back.dto.CreateArticlesRequest;
import com.coloc.back.dto.ShoppingListType;
import com.coloc.back.entity.Article;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ArticleResponse> getArticles(ShoppingListType listType) {
        Long sharedHouseId = currentUserService.getCurrentSharedHouse().getId();

        List<Article> articles = switch (listType) {
            case PERSONAL -> articleRepository.findBySharedHouseIdAndOwnerIdAndBoughtFalseOrderByIdAsc(
                    sharedHouseId, currentUserService.getCurrentRoommateId());
            case SHARED -> articleRepository.findBySharedHouseIdAndOwnerIsNullAndBoughtFalseOrderByIdAsc(sharedHouseId);
        };

        return articles.stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @Transactional
    public List<ArticleResponse> createArticles(CreateArticlesRequest request) {
        SharedHouse sharedHouse = currentUserService.getCurrentSharedHouse();
        Roommate owner = request.list() == ShoppingListType.PERSONAL
                ? currentUserService.getCurrentRoommate()
                : null;

        List<Article> articles = request.names().stream()
                .map(name -> buildArticle(sharedHouse, owner, name))
                .toList();

        return articleRepository.saveAll(articles).stream()
                .map(ArticleResponse::from)
                .toList();
    }

    private Article buildArticle(SharedHouse sharedHouse, Roommate owner, String name) {
        Article article = new Article();
        article.setSharedHouse(sharedHouse);
        article.setOwner(owner);
        article.setName(name.trim());
        article.setBought(false);
        return article;
    }
}