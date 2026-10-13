package com.coloc.back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Une ligne de la table "contribution" : la part d'un participant à une dépense.
 * La part est enregistrée (et non recalculée) pour que les soldes des autres restent justes
 * si ce participant supprime son compte : roommateId passe alors à null, la part reste.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseShare {

    /** null = le participant a supprimé son compte. */
    @Column(name = "roommate_id")
    private Long roommateId;

    @Column(name = "share", nullable = false, precision = 10, scale = 2)
    private BigDecimal share;
}