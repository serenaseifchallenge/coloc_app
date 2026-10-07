package com.coloc.back.dto;

import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;

import java.time.LocalDateTime;
import java.util.List;

public record SharedHouseResponse(
        Long id,
        String name,
        String address,
        String description,
        String invitationCode,
        LocalDateTime creationDate,
        List<RoommateResponse> members
) {
    public static SharedHouseResponse from(SharedHouse h, List<Roommate> members) {
        return new SharedHouseResponse(
                h.getId(),
                h.getName(),
                h.getAddress(),
                h.getDescription(),
                h.getInvitationCode(),
                h.getCreationDate(),
                members.stream().map(RoommateResponse::from).toList()
        );
    }
}