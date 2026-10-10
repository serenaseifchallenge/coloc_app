package com.coloc.back.dto;

import java.math.BigDecimal;

public record TransferResponse(Long fromId, Long toId, BigDecimal amount) {
}
