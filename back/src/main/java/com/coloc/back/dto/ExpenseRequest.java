package com.coloc.back.dto;

import com.coloc.back.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @NotNull LocalDate expenseDate,
        @NotNull Long payerId,
        @NotNull ExpenseCategory category,
        @NotEmpty List<@NotNull Long> participantIds
) {
}
