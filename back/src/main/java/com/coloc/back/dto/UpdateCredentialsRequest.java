package com.coloc.back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** newPassword = null : le mot de passe ne change pas. */
public record UpdateCredentialsRequest(
        @NotBlank @Email String email,
        @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères") String newPassword
) {}
