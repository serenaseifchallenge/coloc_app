package com.coloc.back.repository;

import com.coloc.back.entity.Roommate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoommateRepository extends JpaRepository<Roommate, Long> {

    Optional<Roommate> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Roommate> findBySharedHouseId(Long sharedHouseId);
}