package com.coloc.back.repository;

import com.coloc.back.entity.HiddenTaskName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface HiddenTaskNameRepository extends JpaRepository<HiddenTaskName, Long> {

    @Query("SELECT hidden.name FROM HiddenTaskName hidden WHERE hidden.sharedHouse.id = :sharedHouseId")
    Set<String> findNamesBySharedHouseId(@Param("sharedHouseId") Long sharedHouseId);

    boolean existsBySharedHouseIdAndName(Long sharedHouseId, String name);

    Optional<HiddenTaskName> findBySharedHouseIdAndName(Long sharedHouseId, String name);
}