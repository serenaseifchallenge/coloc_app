package com.coloc.back.dto;

import java.math.BigDecimal;

public record BalanceItemResponse(Long roommateId, String name, String surname, BigDecimal amount) {
}
