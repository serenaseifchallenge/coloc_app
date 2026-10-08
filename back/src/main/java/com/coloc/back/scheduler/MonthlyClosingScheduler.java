package com.coloc.back.scheduler;

import com.coloc.back.service.MonthlyClosingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyClosingScheduler {

    private final MonthlyClosingService monthlyClosingService;

    @EventListener(ApplicationReadyEvent.class)
    public void closeOnStartup() {
        close();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Paris")
    public void closeEveryNight() {
        close();
    }

    private void close() {
        int closedCount = monthlyClosingService.closePreviousMonthIfNeeded();
        if (closedCount > 0) {
            log.info("Clôture du mois : {} score(s) enregistré(s), points remis à zéro", closedCount);
        }
    }
}