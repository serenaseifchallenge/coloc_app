package com.coloc.back.repository;

import com.coloc.back.entity.Roommate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface RoommateRepository extends JpaRepository<Roommate, Long> {

    Optional<Roommate> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Roommate> findBySharedHouseId(Long sharedHouseId);

    @Modifying
    @Query("UPDATE Roommate roommate SET roommate.points = COALESCE(roommate.points, 0) + :points WHERE roommate.id = :roommateId")
    void addPoints(@Param("roommateId") Long roommateId, @Param("points") int points);

    List<Roommate> findBySharedHouseIsNotNull();

    @Modifying
    @Query("UPDATE Roommate roommate SET roommate.points = 0")
    void resetAllPoints();
}