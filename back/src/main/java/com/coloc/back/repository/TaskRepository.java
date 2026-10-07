package com.coloc.back.repository;

import com.coloc.back.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = "assigned")
    List<Task> findBySharedHouseIdAndDone(Long sharedHouseId, boolean done, Sort sort);

    @EntityGraph(attributePaths = "assigned")
    List<Task> findBySharedHouseIdAndAssignedIdAndDone(Long sharedHouseId, Long assignedId, boolean done, Sort sort);
}