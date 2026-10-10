package com.coloc.back.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExpenseSplitter {

    private ExpenseSplitter() {
    }

    public static Map<Long, BigDecimal> splitEqually(BigDecimal amount, List<Long> participantIds) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Le montant ne peut pas avoir plus de 2 décimales");
        }
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("Il faut au moins un participant");
        }
        Set<Long> unique = new HashSet<>(participantIds);
        if (unique.size() != participantIds.size() || unique.contains(null)) {
            throw new IllegalArgumentException("Participants en double ou invalides");
        }

        List<Long> sorted = new ArrayList<>(participantIds);
        sorted.sort(Long::compareTo);

        BigInteger cents = amount.movePointRight(2).toBigIntegerExact();
        BigInteger n = BigInteger.valueOf(sorted.size());
        BigInteger[] qr = cents.divideAndRemainder(n);
        long base = qr[0].longValueExact();
        int remainder = qr[1].intValueExact();

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            long share = base + (i < remainder ? 1 : 0);
            result.put(sorted.get(i), BigDecimal.valueOf(share, 2));
        }
        return result;
    }
}
