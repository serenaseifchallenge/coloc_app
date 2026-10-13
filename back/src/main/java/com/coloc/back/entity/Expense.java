package com.coloc.back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "expense")
@Getter
@Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shared_house_id", nullable = false, updatable = false)
    private Long sharedHouseId;

    /** null = le payeur a supprimé son compte. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private Roommate payer;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    // Table "contribution" : une ligne par participant avec sa part (la colonne status garde sa valeur par défaut)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "contribution", joinColumns = @JoinColumn(name = "expense_id"))
    private List<ExpenseShare> shares = new ArrayList<>();

    /** Participants qui ont encore un compte. */
    public Set<Long> getParticipantIds() {
        return shares.stream()
                .map(ExpenseShare::getRoommateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** Part d'un colocataire (0 s'il ne participe pas). */
    public BigDecimal shareOf(Long roommateId) {
        return shares.stream()
                .filter(share -> roommateId != null && roommateId.equals(share.getRoommateId()))
                .map(ExpenseShare::getShare)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}