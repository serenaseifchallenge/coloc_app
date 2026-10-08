package com.coloc.back.controller;

import com.coloc.back.dto.ArticleResponse;
import com.coloc.back.dto.ShoppingListType;
import com.coloc.back.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}