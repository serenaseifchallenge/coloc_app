package com.coloc.back.dto;

import com.coloc.back.entity.MonthlyScore;

public record PodiumEntryResponse(int rank, RoommateSummaryResponse roommate, int points) {

    public static PodiumEntryResponse from(int rank, MonthlyScore score) {
        return new PodiumEntryResponse(rank, RoommateSummaryResponse.from(score.getRoommate()), score.getPoints());
    }
}