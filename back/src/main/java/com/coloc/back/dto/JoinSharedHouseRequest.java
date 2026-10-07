package com.coloc.back.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinSharedHouseRequest(@NotBlank String invitationCode) {}