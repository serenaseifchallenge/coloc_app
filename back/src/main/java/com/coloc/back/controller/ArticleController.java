package com.coloc.back.controller;

import com.coloc.back.dto.ArticleResponse;
import com.coloc.back.dto.CreateArticlesRequest;
import com.coloc.back.dto.ShoppingListType;
import com.coloc.back.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
}