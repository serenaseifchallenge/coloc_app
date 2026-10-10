package com.coloc.back.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DebtSimplifierTest {

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    @Test
    void seedDataCase() {
        List<Transfer> t = DebtSimplifier.simplify(Map.of(1L, bd("10.00"), 2L, bd("-10.00"), 3L, bd("0.00")));
        assertEquals(List.of(new Transfer(2L, 1L, bd("10.00"))), t);
    }

    @Test
    void caseB() {
        Map<Long, BigDecimal> b = Map.of(1L, bd("34.50"), 2L, bd("12.00"), 3L, bd("-18.20"), 4L, bd("-28.30"));
        List<Transfer> t = DebtSimplifier.simplify(b);
        Set<Transfer> expected = Set.of(
                new Transfer(4L, 1L, bd("28.30")),
                new Transfer(3L, 1L, bd("6.20")),
                new Transfer(3L, 2L, bd("12.00")));
        assertEquals(expected, t.stream().collect(Collectors.toSet()));
        assertEquals(3, t.size());
    }

    @Test
    void allZeroGivesNoTransfer() {
        assertTrue(DebtSimplifier.simplify(Map.of(1L, bd("0.00"), 2L, bd("0"))).isEmpty());
    }

    @Test
    void rejectsNonZeroSum() {
        assertThrows(IllegalArgumentException.class, () -> DebtSimplifier.simplify(Map.of(1L, bd("5"), 2L, bd("-4"))));
    }

    @Test
    void randomPropertiesHold() {
        Random rnd = new Random(42);
        for (int run = 0; run < 500; run++) {
            int n = 2 + rnd.nextInt(7);
            Map<Long, BigDecimal> b = new HashMap<>();
            long sum = 0;
            for (long i = 1; i < n; i++) {
                long c = rnd.nextInt(20001) - 10000;
                sum += c;
                b.put(i, BigDecimal.valueOf(c, 2));
            }
            b.put((long) n, BigDecimal.valueOf(-sum, 2));

            List<Transfer> transfers = DebtSimplifier.simplify(b);
            assertTrue(transfers.size() <= n - 1);

            Map<Long, BigDecimal> after = new HashMap<>(b);
            for (Transfer t : transfers) {
                assertTrue(t.amount().signum() > 0);
                assertNotEquals(t.fromId(), t.toId());
                after.merge(t.fromId(), t.amount(), BigDecimal::add);
                after.merge(t.toId(), t.amount().negate(), BigDecimal::add);
            }
            after.values().forEach(v -> assertEquals(0, v.signum()));
        }
    }
}
