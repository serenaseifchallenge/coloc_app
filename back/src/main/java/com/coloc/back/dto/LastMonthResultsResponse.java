package com.coloc.back.dto;

import java.time.LocalDate;
import java.util.List;

public record LastMonthResultsResponse(LocalDate monthStart, List<PodiumEntryResponse> podium) {
}