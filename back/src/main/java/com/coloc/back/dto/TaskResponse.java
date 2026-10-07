package com.coloc.back.dto;

import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.Task;

import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String name,
        String description,
        LocalDate deadline,
        LocalDate completionDate,
        boolean done,
        Integer points,
        Assignee assignee) {

    public record Assignee(Long id, String name, String surname) {

        static Assignee from(Roommate roommate) {
            if (roommate == null) {
                return null;
            }
            return new Assignee(roommate.getId(), roommate.getName(), roommate.getSurname());
        }
    }

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getDeadline(),
                task.getCompletionDate(),
                task.isDone(),
                task.getPoints(),
                Assignee.from(task.getAssigned()));
    }
}