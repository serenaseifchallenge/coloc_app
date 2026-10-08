package com.coloc.back.service;

import com.coloc.back.dto.ArticleResponse;
import com.coloc.back.dto.CreateArticlesRequest;
import com.coloc.back.dto.ShoppingListType;
import com.coloc.back.dto.UpdateArticleRequest;
import com.coloc.back.entity.Article;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public ArticleResponse updateArticle(Long articleId, UpdateArticleRequest request) {
        Article article = findAccessibleArticle(articleId);
        if (article.isBought()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet article a déjà été acheté");
        }

        article.setName(request.name().trim());
        return ArticleResponse.from(article);
    }

    @Transactional
    public void buyArticle(Long articleId) {
        Article article = findAccessibleArticle(articleId);
        article.setBought(true);
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        Article article = findAccessibleArticle(articleId);
        articleRepository.delete(article);
    }

    private Article findAccessibleArticle(Long articleId) {
        Long sharedHouseId = currentUserService.getCurrentSharedHouse().getId();
        Long currentRoommateId = currentUserService.getCurrentRoommateId();

        return articleRepository.findByIdAndSharedHouseId(articleId, sharedHouseId)
                .filter(article -> isAccessibleBy(article, currentRoommateId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article introuvable"));
    }

    private boolean isAccessibleBy(Article article, Long roommateId) {
        return article.getOwner() == null || roommateId.equals(article.getOwner().getId());
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