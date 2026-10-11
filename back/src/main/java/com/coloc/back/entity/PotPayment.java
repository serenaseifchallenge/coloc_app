package com.coloc.back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pot_payment")
@Getter
@Setter
@NoArgsConstructor
public class PotPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pot_id", nullable = false)
    private Pot pot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roommate_id", nullable = false)
    private Roommate roommate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;
}
