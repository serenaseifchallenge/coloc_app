package com.coloc.back.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank @Size(max = 150) String name,
        @FutureOrPresent LocalDate deadline,
        Long assigneeId,
        @NotNull @Min(1) @Max(100) Integer points
) {
}