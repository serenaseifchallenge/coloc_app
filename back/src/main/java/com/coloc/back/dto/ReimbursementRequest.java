package com.coloc.back.dto;

import com.coloc.back.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReimbursementRequest(
        Long payerId,
        @NotNull Long receiverId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @NotNull PaymentMethod method,
        @NotNull LocalDate reimbursementDate
) {
}
