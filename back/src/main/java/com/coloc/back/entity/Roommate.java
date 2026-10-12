package com.coloc.back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "roommate")
@Getter
@Setter
@NoArgsConstructor
public class Roommate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_house_id")
    private SharedHouse sharedHouse;   // null = pas encore de coloc

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private Integer points;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(nullable = false)
    private LocalDateTime birthday;

    /** Couleur choisie pour l'avatar (ex. « #6c63ff »). null = couleur par défaut. */
    @Column(name = "avatar_color", length = 7)
    private String avatarColor;

    /** Photo de profil en data URL (data:image/jpeg;base64,...). null = initiales. */
    @Column(name = "avatar_image", columnDefinition = "TEXT")
    private String avatarImage;
}