package com.coloc.back.controller;

import com.coloc.back.dto.ReimbursementRequest;
import com.coloc.back.dto.ReimbursementResponse;
import com.coloc.back.service.ReimbursementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reimbursements")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    @GetMapping
    public List<ReimbursementResponse> getLatest(@RequestParam(defaultValue = "10") int limit) {
        return reimbursementService.getLatest(limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReimbursementResponse create(@Valid @RequestBody ReimbursementRequest request) {
        return reimbursementService.create(request);
    }
}
