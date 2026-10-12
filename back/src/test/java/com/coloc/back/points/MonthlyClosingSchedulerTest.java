package com.coloc.back.points;

import com.coloc.back.scheduler.MonthlyClosingScheduler;
import com.coloc.back.service.MonthlyClosingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyClosingSchedulerTest {

    @Mock
    private MonthlyClosingService monthlyClosingService;

    @InjectMocks
    private MonthlyClosingScheduler monthlyClosingScheduler;

    @Test
    @DisplayName("au démarrage de l'application : lance la clôture du mois si besoin")
    void closesOnStartup() {
        when(monthlyClosingService.closePreviousMonthIfNeeded()).thenReturn(3);

        monthlyClosingScheduler.closeOnStartup();

        verify(monthlyClosingService).closePreviousMonthIfNeeded();
    }

    @Test
    @DisplayName("chaque nuit : lance la clôture du mois si besoin")
    void closesEveryNight() {
        when(monthlyClosingService.closePreviousMonthIfNeeded()).thenReturn(0);

        monthlyClosingScheduler.closeEveryNight();

        verify(monthlyClosingService).closePreviousMonthIfNeeded();
    }
}