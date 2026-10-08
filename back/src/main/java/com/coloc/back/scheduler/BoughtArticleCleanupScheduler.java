package com.coloc.back.scheduler;

import com.coloc.back.service.ArticleCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoughtArticleCleanupScheduler {

    private final ArticleCleanupService articleCleanupService;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanUpOnStartup() {
        cleanUp();
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "Europe/Paris")
    public void cleanUpEveryNight() {
        cleanUp();
    }

    private void cleanUp() {
        int deletedCount = articleCleanupService.deleteBoughtArticles();
        log.info("Nettoyage : {} article(s) acheté(s) supprimé(s)", deletedCount);
    }
}