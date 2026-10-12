package com.coloc.back.shopping;

import com.coloc.back.repository.ArticleRepository;
import com.coloc.back.service.ArticleCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleCleanupServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleCleanupService articleCleanupService;

    @Test
    @DisplayName("supprime tous les articles achetés et renvoie leur nombre")
    void deletesBoughtArticles() {
        when(articleRepository.deleteBoughtArticles()).thenReturn(4);

        int deleted = articleCleanupService.deleteBoughtArticles();

        assertThat(deleted).isEqualTo(4);
        verify(articleRepository).deleteBoughtArticles();
    }
}