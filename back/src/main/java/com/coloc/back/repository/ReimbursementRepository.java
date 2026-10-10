package com.coloc.back.repository;

import com.coloc.back.entity.Reimbursement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, Long> {

    @EntityGraph(attributePaths = {"payer", "receiver"})
    List<Reimbursement> findBySharedHouseIdOrderByReimbursementDateDescIdDesc(Long sharedHouseId);

    @EntityGraph(attributePaths = {"payer", "receiver"})
    List<Reimbursement> findBySharedHouseIdOrderByReimbursementDateDescIdDesc(Long sharedHouseId, Pageable pageable);
}
