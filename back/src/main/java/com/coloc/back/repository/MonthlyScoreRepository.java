package com.coloc.back.repository;

import com.coloc.back.entity.MonthlyScore;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyScoreRepository extends JpaRepository<MonthlyScore, Long> {

    boolean existsByMonthStart(LocalDate monthStart);

    @EntityGraph(attributePaths = "roommate")
    List<MonthlyScore> findTop3BySharedHouseIdAndMonthStartAndPointsGreaterThanOrderByPointsDescIdAsc(
            Long sharedHouseId, LocalDate monthStart, Integer minPoints);
}