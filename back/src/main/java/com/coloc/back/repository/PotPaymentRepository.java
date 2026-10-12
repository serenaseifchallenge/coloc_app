package com.coloc.back.repository;

import com.coloc.back.entity.PotPayment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PotPaymentRepository extends JpaRepository<PotPayment, Long> {

    @EntityGraph(attributePaths = "roommate")
    List<PotPayment> findByPotIdInOrderByPaymentDateDescIdDesc(Collection<Long> potIds);

    @EntityGraph(attributePaths = "roommate")
    Optional<PotPayment> findByIdAndPotId(Long id, Long potId);

    void deleteByPotId(Long potId);

    /** Versements d'un coloc dans les cagnottes de la coloc dont l'achat n'est pas encore fait. */
    @EntityGraph(attributePaths = "pot")
    List<PotPayment> findByRoommateIdAndPotSharedHouseIdAndPotPurchasedFalse(Long roommateId, Long sharedHouseId);
}
