package com.coloc.back.controller;

import com.coloc.back.dto.PotPaymentRequest;
import com.coloc.back.dto.PotRequest;
import com.coloc.back.dto.PotResponse;
import com.coloc.back.service.PotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pots")
@RequiredArgsConstructor
public class PotController {

    private final PotService potService;

    @GetMapping
    public List<PotResponse> getPots() {
        return potService.getPots();
    }

    @GetMapping("/{id}")
    public PotResponse getPot(@PathVariable Long id) {
        return potService.getPot(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PotResponse createPot(@Valid @RequestBody PotRequest request) {
        return potService.createPot(request);
    }

    @PutMapping("/{id}")
    public PotResponse updatePot(@PathVariable Long id, @Valid @RequestBody PotRequest request) {
        return potService.updatePot(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePot(@PathVariable Long id) {
        potService.deletePot(id);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PotResponse addPayment(@PathVariable Long id, @Valid @RequestBody PotPaymentRequest request) {
        return potService.addPayment(id, request);
    }
}
