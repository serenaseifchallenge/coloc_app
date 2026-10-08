package com.coloc.back.service;

import com.coloc.back.dto.LastMonthResultsResponse;
import com.coloc.back.dto.PodiumEntryResponse;
import com.coloc.back.entity.MonthlyScore;
import com.coloc.back.repository.MonthlyScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final MonthlyScoreRepository monthlyScoreRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public LastMonthResultsResponse getLastMonthResults() {
        Long sharedHouseId = currentUserService.getCurrentSharedHouse().getId();
        LocalDate previousMonthStart = YearMonth.now().minusMonths(1).atDay(1);

        List<MonthlyScore> topScores = monthlyScoreRepository
                .findTop3BySharedHouseIdAndMonthStartAndPointsGreaterThanOrderByPointsDescIdAsc(
                        sharedHouseId, previousMonthStart, 0);

        List<PodiumEntryResponse> podium = IntStream.range(0, topScores.size())
                .mapToObj(index -> PodiumEntryResponse.from(index + 1, topScores.get(index)))
                .toList();

        return new LastMonthResultsResponse(previousMonthStart, podium);
    }
}