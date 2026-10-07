package com.coloc.back.controller;

import com.coloc.back.dto.RoommateResponse;
import com.coloc.back.dto.UpdateProfileRequest;
import com.coloc.back.service.RoommateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class RoommateController {

    private final RoommateService roommateService;

    @GetMapping
    public RoommateResponse getMe() {
        return roommateService.getMe();
    }

    @PutMapping
    public RoommateResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return roommateService.updateMe(request);
    }
}