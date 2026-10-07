package com.coloc.back.controller;

import com.coloc.back.dto.RoommateResponse;
import com.coloc.back.dto.RoommateSummaryResponse;
import com.coloc.back.dto.UpdateCredentialsRequest;
import com.coloc.back.dto.UpdateProfileRequest;
import com.coloc.back.service.RoommateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PatchMapping("/credentials")
    public RoommateResponse updateCredentials(@Valid @RequestBody UpdateCredentialsRequest request) {
        return roommateService.updateCredentials(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe() {
        roommateService.deleteMe();
    }

    @GetMapping("/roommates")
    public List<RoommateSummaryResponse> getRoommates() {
        return roommateService.getRoommatesOfCurrentSharedHouse();
    }
}