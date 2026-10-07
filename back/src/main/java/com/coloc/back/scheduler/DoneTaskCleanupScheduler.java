package com.coloc.back.scheduler;

import com.coloc.back.service.TaskCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoneTaskCleanupScheduler {

    private final TaskCleanupService taskCleanupService;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanUpOnStartup() {
        cleanUp();
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Paris")
    public void cleanUpEveryNight() {
        cleanUp();
    }

    private void cleanUp() {
        int deletedCount = taskCleanupService.deleteExpiredDoneTasks();
        log.info("Nettoyage : {} tâche(s) faite(s) depuis plus de 14 jours supprimée(s)", deletedCount);
    }
}