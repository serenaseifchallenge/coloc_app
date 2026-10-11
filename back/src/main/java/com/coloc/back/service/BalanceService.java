package com.coloc.back.service;

import com.coloc.back.dto.BalanceItemResponse;
import com.coloc.back.dto.BalanceResponse;
import com.coloc.back.dto.TransferResponse;
import com.coloc.back.entity.Expense;
import com.coloc.back.entity.Reimbursement;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.ExpenseRepository;
import com.coloc.back.repository.ReimbursementRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.util.DebtSimplifier;
import com.coloc.back.util.ExpenseSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final ReimbursementRepository reimbursementRepository;
    private final RoommateRepository roommateRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public BalanceResponse getBalances() {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Map<Long, BigDecimal> balances = computeBalances(houseId);

        Map<Long, Roommate> roommates = roommateRepository.findAllById(balances.keySet()).stream()
                .collect(Collectors.toMap(Roommate::getId, Function.identity()));

        List<BalanceItemResponse> items = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            Roommate roommate = roommates.get(entry.getKey());
            if (roommate != null) {
                items.add(new BalanceItemResponse(
                        roommate.getId(), roommate.getName(), roommate.getSurname(), entry.getValue()));
            }
        }
        items.sort(Comparator.comparing(BalanceItemResponse::amount).reversed()
                .thenComparing(BalanceItemResponse::roommateId));

        List<TransferResponse> transfers = DebtSimplifier.simplify(balances).stream()
                .map(t -> new TransferResponse(t.fromId(), t.toId(), t.amount()))
                .toList();

        return new BalanceResponse(items, transfers);
    }

    /**
     * Solde = ce que j'ai payé - mes parts + remboursements versés - remboursements reçus.
     * Positif : on me doit de l'argent. Négatif : je dois de l'argent. La somme vaut toujours 0.
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> computeBalances(Long houseId) {
        Map<Long, BigDecimal> balances = new TreeMap<>();
        for (Roommate roommate : roommateRepository.findBySharedHouseId(houseId)) {
            balances.put(roommate.getId(), BigDecimal.ZERO.setScale(2));
        }

        for (Expense expense : expenseRepository.findBySharedHouseIdOrderByExpenseDateDescIdDesc(houseId)) {
            if (expense.getParticipantIds().isEmpty()) {
                continue;
            }
            balances.merge(expense.getPayer().getId(), expense.getAmount(), BigDecimal::add);
            Map<Long, BigDecimal> shares = ExpenseSplitter.splitEqually(
                    expense.getAmount(), new ArrayList<>(expense.getParticipantIds()));
            shares.forEach((roommateId, share) -> balances.merge(roommateId, share.negate(), BigDecimal::add));
        }

        for (Reimbursement reimbursement : reimbursementRepository
                .findBySharedHouseIdOrderByReimbursementDateDescIdDesc(houseId)) {
            balances.merge(reimbursement.getPayer().getId(), reimbursement.getAmount(), BigDecimal::add);
            balances.merge(reimbursement.getReceiver().getId(), reimbursement.getAmount().negate(), BigDecimal::add);
        }
        return balances;
    }
}
