package com.coloc.back.dto;

import com.coloc.back.entity.Roommate;

public record RoommateSummaryResponse(Long id, String name, String surname) {

    public static RoommateSummaryResponse from(Roommate roommate) {
        return new RoommateSummaryResponse(roommate.getId(), roommate.getName(), roommate.getSurname());
    }
}