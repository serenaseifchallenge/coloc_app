package com.coloc.back.dto;

import jakarta.validation.constraints.NotNull;

public record PotPurchaseRequest(@NotNull Boolean purchased) {
}
