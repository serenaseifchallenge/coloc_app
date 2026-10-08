package com.coloc.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskNameRequest(
        @NotBlank @Size(max = 150) String name
) {
}