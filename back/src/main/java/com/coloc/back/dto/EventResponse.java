package com.coloc.back.dto;

import com.coloc.back.entity.Event;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean allDay,
        LocalTime startTime,
        LocalTime endTime,
        Long creatorId,
        String creatorName,
        String creatorInitials,
        String creatorAvatarColor,
        Long sharedHouseId
) {
    public static EventResponse fromEntity(Event event) {
        String name = event.getCreator().getName();
        String surname = event.getCreator().getSurname();

        String creatorName = name + " " + surname;

        String creatorInitials = "";
        if (name != null && !name.isBlank()) {
            creatorInitials += name.substring(0, 1).toUpperCase();
        }
        if (surname != null && !surname.isBlank()) {
            creatorInitials += surname.substring(0, 1).toUpperCase();
        }

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartDate(),
                event.getEndDate(),
                event.isAllDay(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCreator().getId(),
                creatorName,
                creatorInitials,
                event.getCreator().getAvatarColor(),
                event.getSharedHouse().getId()
        );
    }
}