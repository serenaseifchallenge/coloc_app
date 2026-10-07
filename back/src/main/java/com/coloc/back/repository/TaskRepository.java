package com.coloc.back.repository;

import com.coloc.back.entity.Task;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = "assigned")
    List<Task> findBySharedHouseIdAndDone(Long sharedHouseId, boolean done, Sort sort);

    @EntityGraph(attributePaths = "assigned")
    List<Task> findBySharedHouseIdAndAssignedIdAndDone(Long sharedHouseId, Long assignedId, boolean done, Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Task> findForUpdateByIdAndSharedHouseId(Long id, Long sharedHouseId);
}