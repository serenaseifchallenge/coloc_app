package com.coloc.back.dto;

import com.coloc.back.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(
        Long id,
        String name,
        BigDecimal amount,
        LocalDate expenseDate,
        ExpenseCategory category,
        RoommateSummaryResponse payer,
        List<Long> participantIds,
        BigDecimal myShare,
        boolean canEdit
) {
}
