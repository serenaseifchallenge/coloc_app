package com.coloc.back.repository;

import com.coloc.back.entity.Pot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PotRepository extends JpaRepository<Pot, Long> {

    @EntityGraph(attributePaths = "participantIds")
    List<Pot> findBySharedHouseIdOrderByIdDesc(Long sharedHouseId);

    @EntityGraph(attributePaths = "participantIds")
    Optional<Pot> findByIdAndSharedHouseId(Long id, Long sharedHouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pot> findForUpdateByIdAndSharedHouseId(Long id, Long sharedHouseId);
}
