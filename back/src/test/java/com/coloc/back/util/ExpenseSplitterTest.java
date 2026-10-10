package com.coloc.back.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpenseSplitterTest {

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    @Test
    void splitsWithRemainderToFirstIds() {
        Map<Long, BigDecimal> r = ExpenseSplitter.splitEqually(bd("54.20"), List.of(3L, 1L, 2L));
        assertEquals(bd("18.07"), r.get(1L));
        assertEquals(bd("18.07"), r.get(2L));
        assertEquals(bd("18.06"), r.get(3L));
    }

    @Test
    void splitsAmongFour() {
        Map<Long, BigDecimal> r = ExpenseSplitter.splitEqually(bd("18.50"), List.of(1L, 2L, 3L, 4L));
        assertEquals(bd("4.63"), r.get(1L));
        assertEquals(bd("4.63"), r.get(2L));
        assertEquals(bd("4.62"), r.get(3L));
        assertEquals(bd("4.62"), r.get(4L));
    }

    @Test
    void sumAlwaysEqualsAmount() {
        for (int cents = 1; cents < 3000; cents += 7) {
            for (int n = 1; n <= 7; n++) {
                BigDecimal amount = BigDecimal.valueOf(cents, 2);
                List<Long> ids = new java.util.ArrayList<>();
                for (long i = 1; i <= n; i++) ids.add(i);
                BigDecimal sum = ExpenseSplitter.splitEqually(amount, ids).values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertEquals(0, amount.compareTo(sum));
            }
        }
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> ExpenseSplitter.splitEqually(bd("0"), List.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> ExpenseSplitter.splitEqually(bd("-5"), List.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> ExpenseSplitter.splitEqually(null, List.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> ExpenseSplitter.splitEqually(bd("10.001"), List.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> ExpenseSplitter.splitEqually(bd("10"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> ExpenseSplitter.splitEqually(bd("10"), List.of(1L, 1L)));
    }
}
