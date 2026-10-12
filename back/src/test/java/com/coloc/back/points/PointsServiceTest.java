package com.coloc.back.points;

import com.coloc.back.dto.LastMonthResultsResponse;
import com.coloc.back.dto.PodiumEntryResponse;
import com.coloc.back.entity.MonthlyScore;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.MonthlyScoreRepository;
import com.coloc.back.service.CurrentUserService;
import com.coloc.back.service.PointsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

    private static final Long HOUSE_ID = 1L;

    @Mock
    private MonthlyScoreRepository monthlyScoreRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PointsService pointsService;

    private SharedHouse house;
    private LocalDate previousMonthStart;

    @BeforeEach
    void setUp() {
        house = new SharedHouse();
        house.setId(HOUSE_ID);
        previousMonthStart = YearMonth.now().minusMonths(1).atDay(1);
        when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
    }

    @Test
    @DisplayName("podium : rangs 1, 2, 3 dans l'ordre renvoyé par le repository, avec noms et points")
    void buildsPodiumWithRanksInRepositoryOrder() {
        MonthlyScore first = score(roommate(10L, "Emma", "Martin"), 120);
        MonthlyScore second = score(roommate(11L, "Lucas", "Bernard"), 80);
        MonthlyScore third = score(roommate(12L, "Inès", "Petit"), 45);
        when(monthlyScoreRepository.findTop3BySharedHouseIdAndMonthStartAndPointsGreaterThanOrderByPointsDescIdAsc(
                HOUSE_ID, previousMonthStart, 0))
                .thenReturn(List.of(first, second, third));

        LastMonthResultsResponse result = pointsService.getLastMonthResults();

        assertThat(result.monthStart()).isEqualTo(previousMonthStart);
        assertThat(result.podium()).extracting(PodiumEntryResponse::rank).containsExactly(1, 2, 3);
        assertThat(result.podium()).extracting(PodiumEntryResponse::points).containsExactly(120, 80, 45);
        assertThat(result.podium()).extracting(entry -> entry.roommate().name())
                .containsExactly("Emma", "Lucas", "Inès");
    }

    @Test
    @DisplayName("seulement 2 colocataires ont marqué des points : podium de 2 places")
    void buildsShorterPodiumWhenFewerThanThreeScores() {
        when(monthlyScoreRepository.findTop3BySharedHouseIdAndMonthStartAndPointsGreaterThanOrderByPointsDescIdAsc(
                HOUSE_ID, previousMonthStart, 0))
                .thenReturn(List.of(score(roommate(10L, "Emma", "Martin"), 30), score(roommate(11L, "Lucas", "Bernard"), 10)));

        LastMonthResultsResponse result = pointsService.getLastMonthResults();

        assertThat(result.podium()).hasSize(2);
        assertThat(result.podium().get(1).rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("personne n'a marqué de points (ou première utilisation) : podium vide")
    void returnsEmptyPodiumWhenNoScores() {
        when(monthlyScoreRepository.findTop3BySharedHouseIdAndMonthStartAndPointsGreaterThanOrderByPointsDescIdAsc(
                HOUSE_ID, previousMonthStart, 0))
                .thenReturn(List.of());

        LastMonthResultsResponse result = pointsService.getLastMonthResults();

        assertThat(result.monthStart()).isEqualTo(previousMonthStart);
        assertThat(result.podium()).isEmpty();
    }


    private static Roommate roommate(Long id, String name, String surname) {
        Roommate roommate = new Roommate();
        roommate.setId(id);
        roommate.setName(name);
        roommate.setSurname(surname);
        return roommate;
    }

    private MonthlyScore score(Roommate roommate, int points) {
        MonthlyScore score = new MonthlyScore();
        score.setSharedHouse(house);
        score.setRoommate(roommate);
        score.setMonthStart(previousMonthStart);
        score.setPoints(points);
        return score;
    }
}