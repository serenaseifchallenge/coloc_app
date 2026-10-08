package com.coloc.back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "custom_task_name")
@Getter
@Setter
@NoArgsConstructor
public class CustomTaskName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_house_id", nullable = false)
    private SharedHouse sharedHouse;

    @Column(nullable = false, length = 150)
    private String name;
}