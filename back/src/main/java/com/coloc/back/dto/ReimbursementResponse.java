package com.coloc.back.dto;

import com.coloc.back.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReimbursementResponse(
        Long id,
        RoommateSummaryResponse payer,
        RoommateSummaryResponse receiver,
        BigDecimal amount,
        PaymentMethod method,
        LocalDate reimbursementDate
) {
}
