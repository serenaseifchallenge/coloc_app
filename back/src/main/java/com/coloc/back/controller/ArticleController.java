package com.coloc.back.controller;

import com.coloc.back.dto.ArticleResponse;
import com.coloc.back.dto.CreateArticlesRequest;
import com.coloc.back.dto.ShoppingListType;
import com.coloc.back.dto.UpdateArticleRequest;
import com.coloc.back.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public List<ArticleResponse> getArticles(@RequestParam("list") ShoppingListType listType) {
        return articleService.getArticles(listType);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ArticleResponse> createArticles(@Valid @RequestBody CreateArticlesRequest request) {
        return articleService.createArticles(request);
    }

    @PatchMapping("/{id}")
    public ArticleResponse updateArticle(@PathVariable Long id, @Valid @RequestBody UpdateArticleRequest request) {
        return articleService.updateArticle(id, request);
    }

    @PatchMapping("/{id}/buy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void buyArticle(@PathVariable Long id) {
        articleService.buyArticle(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
    }
}