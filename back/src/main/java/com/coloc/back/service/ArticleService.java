package com.coloc.back.service;

import com.coloc.back.dto.ArticleResponse;
import com.coloc.back.dto.ShoppingListType;
import com.coloc.back.entity.Article;
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
}