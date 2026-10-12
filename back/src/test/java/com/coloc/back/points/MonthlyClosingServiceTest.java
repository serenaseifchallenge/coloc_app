package com.coloc.back.points;

import com.coloc.back.entity.MonthlyScore;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.MonthlyScoreRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.service.MonthlyClosingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyClosingServiceTest {

    @Mock
    private RoommateRepository roommateRepository;

    @Mock
    private MonthlyScoreRepository monthlyScoreRepository;

    @InjectMocks
    private MonthlyClosingService monthlyClosingService;

    @Captor
    private ArgumentCaptor<List<MonthlyScore>> scoresCaptor;

    private LocalDate previousMonthStart;

    @BeforeEach
    void setUp() {
        previousMonthStart = YearMonth.now().minusMonths(1).atDay(1);
    }

    @Test
    @DisplayName("mois précédent déjà clôturé : ne fait rien et renvoie 0")
    void doesNothingWhenPreviousMonthIsAlreadyClosed() {
        when(monthlyScoreRepository.existsByMonthStart(previousMonthStart)).thenReturn(true);

        int closed = monthlyClosingService.closePreviousMonthIfNeeded();

        assertThat(closed).isZero();
        verify(roommateRepository, never()).findBySharedHouseIsNotNull();
        verify(monthlyScoreRepository, never()).saveAll(any());
        verify(roommateRepository, never()).resetAllPoints();
    }

    @Test
    @DisplayName("mois pas encore clôturé : un score par colocataire, puis remise à zéro des points")
    void savesOneScorePerRoommateThenResetsPoints() {
        SharedHouse lilas = house(1L);
        SharedHouse tilleuls = house(2L);
        Roommate emma = roommate(10L, lilas, 120);
        Roommate lucas = roommate(11L, lilas, 45);
        Roommate ines = roommate(12L, tilleuls, 80);
        when(monthlyScoreRepository.existsByMonthStart(previousMonthStart)).thenReturn(false);
        when(roommateRepository.findBySharedHouseIsNotNull()).thenReturn(List.of(emma, lucas, ines));

        int closed = monthlyClosingService.closePreviousMonthIfNeeded();

        assertThat(closed).isEqualTo(3);

        InOrder order = inOrder(monthlyScoreRepository, roommateRepository);
        order.verify(monthlyScoreRepository).saveAll(scoresCaptor.capture());
        order.verify(roommateRepository).resetAllPoints();

        List<MonthlyScore> scores = scoresCaptor.getValue();
        assertThat(scores).extracting(MonthlyScore::getRoommate).containsExactly(emma, lucas, ines);
        assertThat(scores).extracting(MonthlyScore::getPoints).containsExactly(120, 45, 80);
        assertThat(scores).extracting(MonthlyScore::getSharedHouse).containsExactly(lilas, lilas, tilleuls);
        assertThat(scores).allSatisfy(score -> assertThat(score.getMonthStart()).isEqualTo(previousMonthStart));
    }

    @Test
    @DisplayName("points null ou négatifs : enregistrés à 0")
    void storesNullOrNegativePointsAsZero() {
        SharedHouse house = house(1L);
        Roommate newcomer = roommate(10L, house, null);
        Roommate unlucky = roommate(11L, house, -15);
        when(monthlyScoreRepository.existsByMonthStart(previousMonthStart)).thenReturn(false);
        when(roommateRepository.findBySharedHouseIsNotNull()).thenReturn(List.of(newcomer, unlucky));

        monthlyClosingService.closePreviousMonthIfNeeded();

        verify(monthlyScoreRepository).saveAll(scoresCaptor.capture());
        assertThat(scoresCaptor.getValue()).extracting(MonthlyScore::getPoints).containsExactly(0, 0);
    }

    @Test
    @DisplayName("aucun colocataire en coloc : aucun score, renvoie 0")
    void savesNothingWhenNoRoommateHasAHouse() {
        when(monthlyScoreRepository.existsByMonthStart(previousMonthStart)).thenReturn(false);
        when(roommateRepository.findBySharedHouseIsNotNull()).thenReturn(List.of());

        int closed = monthlyClosingService.closePreviousMonthIfNeeded();

        assertThat(closed).isZero();
        verify(monthlyScoreRepository).saveAll(scoresCaptor.capture());
        assertThat(scoresCaptor.getValue()).isEmpty();
    }

    private static SharedHouse house(Long id) {
        SharedHouse house = new SharedHouse();
        house.setId(id);
        return house;
    }

    private static Roommate roommate(Long id, SharedHouse house, Integer points) {
        Roommate roommate = new Roommate();
        roommate.setId(id);
        roommate.setSharedHouse(house);
        roommate.setPoints(points);
        return roommate;
    }
}