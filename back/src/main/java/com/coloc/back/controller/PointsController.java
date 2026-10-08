package com.coloc.back.controller;

import com.coloc.back.dto.LastMonthResultsResponse;
import com.coloc.back.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @GetMapping("/last-month")
    public LastMonthResultsResponse getLastMonthResults() {
        return pointsService.getLastMonthResults();
    }
}