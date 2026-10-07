package com.coloc.back.service;

import com.coloc.back.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TaskCleanupService {

    static final int DONE_TASK_RETENTION_DAYS = 14;

    private final TaskRepository taskRepository;

    @Transactional
    public int deleteExpiredDoneTasks() {
        LocalDate limitDate = LocalDate.now().minusDays(DONE_TASK_RETENTION_DAYS);
        return taskRepository.deleteDoneTasksCompletedBefore(limitDate);
    }
}