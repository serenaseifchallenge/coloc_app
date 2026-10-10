package com.coloc.back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
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

    // Table "contribution" : une ligne par participant (la colonne status garde sa valeur par défaut)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "contribution", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "roommate_id", nullable = false)
    private Set<Long> participantIds = new HashSet<>();
}
