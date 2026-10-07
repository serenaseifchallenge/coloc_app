package com.coloc.back.service;

import com.coloc.back.dto.TaskResponse;
import com.coloc.back.entity.Task;
import com.coloc.back.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Sort TO_DO_SORT = Sort.by(Sort.Direction.ASC, "deadline");
    private static final Sort DONE_SORT = Sort.by(Sort.Direction.DESC, "completionDate");

    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(boolean done, boolean assignedToMe) {
        Long sharedHouseId = currentUserService.getCurrentSharedHouse().getId();
        Sort sort = done ? DONE_SORT : TO_DO_SORT;

        List<Task> tasks = assignedToMe
                ? taskRepository.findBySharedHouseIdAndAssignedIdAndDone(
                        sharedHouseId, currentUserService.getCurrentRoommateId(), done, sort)
                : taskRepository.findBySharedHouseIdAndDone(sharedHouseId, done, sort);

        return tasks.stream()
                .map(TaskResponse::from)
                .toList();
    }
}