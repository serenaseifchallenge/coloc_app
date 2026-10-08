package com.coloc.back.repository;

import com.coloc.back.entity.CustomTaskName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomTaskNameRepository extends JpaRepository<CustomTaskName, Long> {

    @Query("SELECT custom.name FROM CustomTaskName custom WHERE custom.sharedHouse.id = :sharedHouseId ORDER BY custom.id")
    List<String> findNamesBySharedHouseId(@Param("sharedHouseId") Long sharedHouseId);

    Optional<CustomTaskName> findBySharedHouseIdAndNameIgnoreCase(Long sharedHouseId, String name);
}