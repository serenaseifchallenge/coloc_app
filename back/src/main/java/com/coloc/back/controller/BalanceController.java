package com.coloc.back.controller;

import com.coloc.back.dto.BalanceResponse;
import com.coloc.back.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping
    public BalanceResponse getBalances() {
        return balanceService.getBalances();
    }
}
