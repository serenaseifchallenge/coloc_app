package com.coloc.back.repository;

import com.coloc.back.entity.PotPayment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PotPaymentRepository extends JpaRepository<PotPayment, Long> {

    @EntityGraph(attributePaths = "roommate")
    List<PotPayment> findByPotIdInOrderByPaymentDateDescIdDesc(Collection<Long> potIds);

    void deleteByPotId(Long potId);
}
