package com.coloc.back.dto;

import com.coloc.back.entity.Roommate;

import java.time.LocalDate;

public record RoommateResponse(
        Long id,
        String name,
        String surname,
        String email,
        Integer points,
        LocalDate birthday,
        Long sharedHouseId
) {
    public static RoommateResponse from(Roommate r) {
        return new RoommateResponse(
                r.getId(),
                r.getName(),
                r.getSurname(),
                r.getEmail(),
                r.getPoints(),
                r.getBirthday().toLocalDate(),
                r.getSharedHouse() != null ? r.getSharedHouse().getId() : null
        );
    }
}