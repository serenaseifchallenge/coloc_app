package com.coloc.back.dto;

import java.math.BigDecimal;

public record PotParticipantResponse(RoommateSummaryResponse roommate, BigDecimal paid, BigDecimal share) {
}
