package com.coloc.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 150, message = "Le titre ne peut pas dépasser 150 caractères")
        String title,

        String description,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate startDate,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate endDate,

        boolean allDay,

        LocalTime startTime,

        LocalTime endTime,

        @NotNull(message = "Le créateur est obligatoire")
        Long creatorId,

        @NotNull(message = "La colocation est obligatoire")
        Long sharedHouseId
) {
}