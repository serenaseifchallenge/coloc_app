package com.coloc.back.dto;

import com.coloc.back.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PotPaymentResponse(
        Long id,
        RoommateSummaryResponse roommate,
        BigDecimal amount,
        PaymentMethod method,
        LocalDate paymentDate
) {
}
