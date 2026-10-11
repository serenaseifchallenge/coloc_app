package com.coloc.back.util;

import java.math.BigDecimal;

public record Transfer(Long fromId, Long toId, BigDecimal amount) {
}
