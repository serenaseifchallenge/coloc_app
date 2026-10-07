package com.coloc.back.controller;

import com.coloc.back.dto.JoinSharedHouseRequest;
import com.coloc.back.dto.SharedHouseRequest;
import com.coloc.back.dto.SharedHouseResponse;
import com.coloc.back.service.SharedHouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shared-houses")
@RequiredArgsConstructor
public class SharedHouseController {

    private final SharedHouseService sharedHouseService;

    @GetMapping("/mine")
    public SharedHouseResponse getMine() {
        return sharedHouseService.getMine();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SharedHouseResponse create(@Valid @RequestBody SharedHouseRequest request) {
        return sharedHouseService.create(request);
    }

    @PutMapping("/mine")
    public SharedHouseResponse update(@Valid @RequestBody SharedHouseRequest request) {
        return sharedHouseService.update(request);
    }

    @PostMapping("/join")
    public SharedHouseResponse join(@Valid @RequestBody JoinSharedHouseRequest request) {
        return sharedHouseService.join(request);
    }

    @PostMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave() {
        sharedHouseService.leave();
    }
}