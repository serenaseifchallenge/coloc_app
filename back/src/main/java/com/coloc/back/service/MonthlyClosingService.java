package com.coloc.back.service;

import com.coloc.back.entity.MonthlyScore;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.MonthlyScoreRepository;
import com.coloc.back.repository.RoommateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyClosingService {

    private final RoommateRepository roommateRepository;
    private final MonthlyScoreRepository monthlyScoreRepository;

    @Transactional
    public int closePreviousMonthIfNeeded() {
        LocalDate previousMonthStart = YearMonth.now().minusMonths(1).atDay(1);
        if (monthlyScoreRepository.existsByMonthStart(previousMonthStart)) {
            return 0;
        }

        List<MonthlyScore> scores = roommateRepository.findBySharedHouseIsNotNull().stream()
                .map(roommate -> buildScore(roommate, previousMonthStart))
                .toList();

        monthlyScoreRepository.saveAll(scores);
        roommateRepository.resetAllPoints();
        return scores.size();
    }

    private MonthlyScore buildScore(Roommate roommate, LocalDate monthStart) {
        MonthlyScore score = new MonthlyScore();
        score.setSharedHouse(roommate.getSharedHouse());
        score.setRoommate(roommate);
        score.setMonthStart(monthStart);
        score.setPoints(Math.max(roommate.getPoints() == null ? 0 : roommate.getPoints(), 0));
        return score;
    }
}