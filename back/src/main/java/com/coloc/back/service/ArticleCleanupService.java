package com.coloc.back.service;

import com.coloc.back.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleCleanupService {

    private final ArticleRepository articleRepository;

    @Transactional
    public int deleteBoughtArticles() {
        return articleRepository.deleteBoughtArticles();
    }
}