package com.coloc.back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "monthly_score")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_house_id", nullable = false)
    private SharedHouse sharedHouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roommate_id", nullable = false)
    private Roommate roommate;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(nullable = false)
    private Integer points;
}