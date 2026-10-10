package com.coloc.back.dto;

import java.math.BigDecimal;

public record ExpenseSummaryResponse(String month, BigDecimal monthTotal, BigDecimal myShare, BigDecimal myBalance) {
}
