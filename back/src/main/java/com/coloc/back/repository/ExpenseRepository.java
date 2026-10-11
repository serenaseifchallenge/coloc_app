package com.coloc.back.repository;

import com.coloc.back.entity.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @EntityGraph(attributePaths = {"payer", "participantIds"})
    List<Expense> findBySharedHouseIdOrderByExpenseDateDescIdDesc(Long sharedHouseId);

    @EntityGraph(attributePaths = {"payer", "participantIds"})
    List<Expense> findBySharedHouseIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(
            Long sharedHouseId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"payer", "participantIds"})
    Optional<Expense> findByIdAndSharedHouseId(Long id, Long sharedHouseId);
}
