package com.coloc.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateArticlesRequest(
        @NotNull ShoppingListType list,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 100) String> names
) {
}