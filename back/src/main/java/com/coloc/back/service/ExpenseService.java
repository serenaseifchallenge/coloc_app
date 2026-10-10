package com.coloc.back.service;

import com.coloc.back.dto.ExpenseRequest;
import com.coloc.back.dto.ExpenseResponse;
import com.coloc.back.dto.ExpenseSummaryResponse;
import com.coloc.back.dto.RoommateSummaryResponse;
import com.coloc.back.entity.Expense;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.ExpenseRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.util.ExpenseSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

    private final ExpenseRepository expenseRepository;
    private final RoommateRepository roommateRepository;
    private final CurrentUserService currentUserService;
    private final BalanceService balanceService;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(YearMonth month) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Long meId = currentUserService.getCurrentRoommateId();

        return expenseRepository
                .findBySharedHouseIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(
                        houseId, month.atDay(1), month.atEndOfMonth())
                .stream()
                .map(expense -> toResponse(expense, meId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getSummary(YearMonth month) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Long meId = currentUserService.getCurrentRoommateId();

        List<Expense> expenses = expenseRepository
                .findBySharedHouseIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(
                        houseId, month.atDay(1), month.atEndOfMonth());

        BigDecimal total = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal myShare = expenses.stream().map(expense -> shareOf(expense, meId)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal myBalance = balanceService.computeBalances(houseId).getOrDefault(meId, BigDecimal.ZERO);

        return new ExpenseSummaryResponse(month.toString(), total, myShare, myBalance);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(Long id) {
        return toResponse(findExpense(id), currentUserService.getCurrentRoommateId());
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Roommate payer = validate(request, houseId);

        Expense expense = new Expense();
        expense.setSharedHouseId(houseId);
        apply(expense, request, payer);

        return toResponse(expenseRepository.save(expense), currentUserService.getCurrentRoommateId());
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Expense expense = findEditableExpense(id);
        Roommate payer = validate(request, houseId);

        apply(expense, request, payer);
        return toResponse(expense, currentUserService.getCurrentRoommateId());
    }

    @Transactional
    public void deleteExpense(Long id) {
        expenseRepository.delete(findEditableExpense(id));
    }

    private Expense findExpense(Long id) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        return expenseRepository.findByIdAndSharedHouseId(id, houseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dépense introuvable"));
    }

    private Expense findEditableExpense(Long id) {
        Expense expense = findExpense(id);
        if (!expense.getPayer().getId().equals(currentUserService.getCurrentRoommateId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul le payeur peut modifier ou supprimer cette dépense");
        }
        return expense;
    }

    private Roommate validate(ExpenseRequest request, Long houseId) {
        if (request.amount().compareTo(MAX_AMOUNT) > 0) {
            throw badRequest("Le montant est trop élevé");
        }

        Map<Long, Roommate> members = roommateRepository.findBySharedHouseId(houseId).stream()
                .collect(Collectors.toMap(Roommate::getId, Function.identity()));

        Roommate payer = members.get(request.payerId());
        if (payer == null) {
            throw badRequest("Le payeur ne fait pas partie de votre colocation");
        }

        Set<Long> unique = new HashSet<>(request.participantIds());
        if (unique.size() != request.participantIds().size()) {
            throw badRequest("Un participant est présent plusieurs fois");
        }
        if (!members.keySet().containsAll(unique)) {
            throw badRequest("Un participant ne fait pas partie de votre colocation");
        }
        return payer;
    }

    private void apply(Expense expense, ExpenseRequest request, Roommate payer) {
        expense.setName(request.name().trim());
        expense.setAmount(request.amount().setScale(2));
        expense.setExpenseDate(request.expenseDate());
        expense.setCategory(request.category());
        expense.setPayer(payer);
        expense.getParticipantIds().clear();
        expense.getParticipantIds().addAll(request.participantIds());
    }

    private BigDecimal shareOf(Expense expense, Long roommateId) {
        if (!expense.getParticipantIds().contains(roommateId)) {
            return BigDecimal.ZERO;
        }
        return ExpenseSplitter.splitEqually(expense.getAmount(), new ArrayList<>(expense.getParticipantIds()))
                .get(roommateId);
    }

    private ExpenseResponse toResponse(Expense expense, Long meId) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getName(),
                expense.getAmount(),
                expense.getExpenseDate(),
                expense.getCategory(),
                RoommateSummaryResponse.from(expense.getPayer()),
                expense.getParticipantIds().stream().sorted().toList(),
                shareOf(expense, meId),
                expense.getPayer().getId().equals(meId)
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
