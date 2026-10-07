package com.coloc.back.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String surname,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères") String password,
        @NotNull @Past LocalDate birthday
) {}
