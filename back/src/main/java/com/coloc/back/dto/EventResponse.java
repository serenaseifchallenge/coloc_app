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

        Long sharedHouseId
) {

    public static EventResponse fromEntity(Event event) {

        String creatorName =
                event.getCreator().getName() + " " + event.getCreator().getSurname();

        String creatorInitials = "";

        if (event.getCreator().getName() != null
                && !event.getCreator().getName().isBlank()) {
            creatorInitials +=
                    event.getCreator().getName().substring(0, 1).toUpperCase();
        }

        if (event.getCreator().getSurname() != null
                && !event.getCreator().getSurname().isBlank()) {
            creatorInitials +=
                    event.getCreator().getSurname().substring(0, 1).toUpperCase();
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
                event.getSharedHouse().getId()
        );
    }
}