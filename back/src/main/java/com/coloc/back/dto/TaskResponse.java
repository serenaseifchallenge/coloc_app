package com.coloc.back.dto;

import com.coloc.back.entity.Task;

import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String name,
        LocalDate deadline,
        LocalDate completionDate,
        boolean done,
        Integer points,
        RoommateSummaryResponse assignee
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getDeadline(),
                task.getCompletionDate(),
                task.isDone(),
                task.getPoints(),
                task.getAssigned() == null ? null : RoommateSummaryResponse.from(task.getAssigned())
        );
    }
}