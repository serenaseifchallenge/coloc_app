package com.coloc.back.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String surname,
        @NotNull @Past LocalDate birthday
) {}