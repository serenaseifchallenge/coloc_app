package com.coloc.back.dto;

import com.coloc.back.entity.PotType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PotResponse(
        Long id,
        String name,
        PotType type,
        BigDecimal targetAmount,
        LocalDate deadline,
        BigDecimal sharePerPerson,
        BigDecimal collected,
        int percent,
        BigDecimal remaining,
        BigDecimal remainingPerPerson,
        boolean participant,
        BigDecimal myShare,
        BigDecimal myPaid,
        boolean canEdit,
        List<Long> participantIds,
        List<PotParticipantResponse> participants,
        List<PotPaymentResponse> payments
) {
}
