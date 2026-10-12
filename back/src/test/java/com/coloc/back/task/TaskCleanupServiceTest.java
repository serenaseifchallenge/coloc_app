package com.coloc.back.task;

import com.coloc.back.repository.TaskRepository;
import com.coloc.back.service.TaskCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCleanupServiceTest {

    private static final int RETENTION_DAYS = 14;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskCleanupService taskCleanupService;


    @Test
    @DisplayName("supprime les tâches faites depuis plus de 14 jours et renvoie leur nombre")
    void deletesDoneTasksOlderThanRetention() {
        LocalDate expectedLimit = LocalDate.now().minusDays(RETENTION_DAYS);
        when(taskRepository.deleteDoneTasksCompletedBefore(expectedLimit)).thenReturn(3);

        int deleted = taskCleanupService.deleteExpiredDoneTasks();

        assertThat(deleted).isEqualTo(3);
        verify(taskRepository).deleteDoneTasksCompletedBefore(expectedLimit);
    }
}