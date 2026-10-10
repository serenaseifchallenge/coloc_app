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
@Table(name = "pot")
@Getter
@Setter
@NoArgsConstructor
public class Pot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shared_house_id", nullable = false, updatable = false)
    private Long sharedHouseId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdById;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PotType type;

    @Column(name = "target_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetAmount;

    private LocalDate deadline;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "pot_participant", joinColumns = @JoinColumn(name = "pot_id"))
    @Column(name = "roommate_id", nullable = false)
    private Set<Long> participantIds = new HashSet<>();
}
