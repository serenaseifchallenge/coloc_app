package com.coloc.back.service;

import com.coloc.back.dto.CreateTaskRequest;
import com.coloc.back.dto.TaskResponse;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.entity.Task;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TaskService {

        private static final Sort TO_DO_SORT = Sort.by(Sort.Direction.ASC, "deadline");
        private static final Sort DONE_SORT = Sort.by(Sort.Direction.DESC, "completionDate");

        private final TaskRepository taskRepository;
        private final RoommateRepository roommateRepository;
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

        @Transactional
        public TaskResponse createTask(CreateTaskRequest request) {
                SharedHouse sharedHouse = currentUserService.getCurrentSharedHouse();

                Task task = new Task();
                task.setSharedHouse(sharedHouse);
                task.setName(request.name().trim());
                task.setDeadline(request.deadline());
                task.setPoints(request.points());
                task.setDone(false);
                task.setAssigned(findAssignee(request.assigneeId(), sharedHouse.getId()));

                return TaskResponse.from(taskRepository.save(task));
        }

        private Roommate findAssignee(Long assigneeId, Long sharedHouseId) {
                if (assigneeId == null) {
                        return null;
                }
                return roommateRepository.findById(assigneeId)
                                .filter(roommate -> isMemberOf(roommate, sharedHouseId))
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Ce colocataire ne fait pas partie de votre colocation"));
        }

        private boolean isMemberOf(Roommate roommate, Long sharedHouseId) {
                return roommate.getSharedHouse() != null
                                && sharedHouseId.equals(roommate.getSharedHouse().getId());
        }

        @Transactional
        public TaskResponse completeTask(Long taskId) {
                Long sharedHouseId = currentUserService.getCurrentSharedHouse().getId();

                Task task = taskRepository.findForUpdateByIdAndSharedHouseId(taskId, sharedHouseId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Tâche introuvable"));

                if (task.isDone()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette tâche est déjà faite");
                }
                if (task.getAssigned() == null) {
                        task.setAssigned(currentUserService.getCurrentRoommate());
                }

                task.setDone(true);
                task.setCompletionDate(LocalDate.now());
                roommateRepository.addPoints(task.getAssigned().getId(), task.getPoints());

                return TaskResponse.from(task);
        }
}