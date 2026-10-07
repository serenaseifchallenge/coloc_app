package com.coloc.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SharedHouseRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 255) String address,
        @NotBlank String description
) {}