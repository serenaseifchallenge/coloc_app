package com.coloc.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateArticleRequest(
        @NotBlank @Size(max = 100) String name
) {
}