package com.coloc.back.service;

import com.coloc.back.dto.TaskResponse;
import com.coloc.back.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(boolean done) {
        Long sharedHouseId = currentUserService.getCurrentSharedHouse().getId();

        return taskRepository.findBySharedHouseIdAndDoneOrderByDeadlineAsc(sharedHouseId, done)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }
}