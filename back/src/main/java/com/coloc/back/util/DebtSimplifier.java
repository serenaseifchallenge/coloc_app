package com.coloc.back.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class DebtSimplifier {

    private DebtSimplifier() {
    }

    public static List<Transfer> simplify(Map<Long, BigDecimal> balances) {
        BigDecimal total = balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() != 0) {
            throw new IllegalArgumentException("La somme des soldes doit être égale à 0 (reçu : " + total + ")");
        }

        List<long[]> creditors = new ArrayList<>();
        List<long[]> debtors = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : balances.entrySet()) {
            long cents = e.getValue().movePointRight(2).longValueExact();
            if (cents > 0) {
                creditors.add(new long[]{e.getKey(), cents});
            } else if (cents < 0) {
                debtors.add(new long[]{e.getKey(), -cents});
            }
        }

        Comparator<long[]> order = Comparator.<long[]>comparingLong(a -> -a[1]).thenComparingLong(a -> a[0]);
        List<Transfer> transfers = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            creditors.sort(order);
            debtors.sort(order);
            long[] creditor = creditors.get(0);
            long[] debtor = debtors.get(0);
            long amount = Math.min(creditor[1], debtor[1]);
            transfers.add(new Transfer(debtor[0], creditor[0], BigDecimal.valueOf(amount, 2)));
            creditor[1] -= amount;
            debtor[1] -= amount;
            if (creditor[1] == 0) {
                creditors.remove(0);
            }
            if (debtor[1] == 0) {
                debtors.remove(0);
            }
        }
        return transfers;
    }
}
