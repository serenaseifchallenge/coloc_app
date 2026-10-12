package com.coloc.back.task;

import com.coloc.back.dto.CreateTaskRequest;
import com.coloc.back.dto.TaskResponse;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.entity.Task;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.repository.TaskRepository;
import com.coloc.back.service.CurrentUserService;
import com.coloc.back.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long HOUSE_ID = 1L;
    private static final Long OTHER_HOUSE_ID = 2L;
    private static final Long TASK_ID = 10L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RoommateRepository roommateRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<Task> taskCaptor;


    @Nested
    @DisplayName("getTasks")
    class GetTasks {

        @Test
        @DisplayName("tâches à faire de toute la coloc : triées par échéance croissante")
        void returnsToDoTasksOfHouseSortedByDeadline() {
            SharedHouse house = house(HOUSE_ID);
            Task task = task(house, null, false, 5);
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(taskRepository.findBySharedHouseIdAndDone(HOUSE_ID, false, Sort.by(Sort.Direction.ASC, "deadline")))
                    .thenReturn(List.of(task));

            List<TaskResponse> result = taskService.getTasks(false, false);

            assertThat(result).extracting(TaskResponse::id).containsExactly(TASK_ID);
            verify(currentUserService, never()).getCurrentRoommateId();
        }

        @Test
        @DisplayName("mes tâches faites : filtrées sur moi et triées par date de réalisation décroissante")
        void returnsMyDoneTasksSortedByCompletionDateDesc() {
            SharedHouse house = house(HOUSE_ID);
            Roommate me = roommate(5L, house);
            Task task = task(house, me, true, 5);
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(currentUserService.getCurrentRoommateId()).thenReturn(me.getId());
            when(taskRepository.findBySharedHouseIdAndAssignedIdAndDone(
                    HOUSE_ID, me.getId(), true, Sort.by(Sort.Direction.DESC, "completionDate")))
                    .thenReturn(List.of(task));

            List<TaskResponse> result = taskService.getTasks(true, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).assignee().id()).isEqualTo(me.getId());
        }
    }


    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("sans assigné : nom nettoyé, tâche à faire, rattachée à ma coloc")
        void createsUnassignedTask() {
            SharedHouse house = house(HOUSE_ID);
            LocalDate deadline = LocalDate.now().plusDays(3);
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse result = taskService.createTask(new CreateTaskRequest("  Faire la vaisselle  ", deadline, null, 10));

            verify(taskRepository).save(taskCaptor.capture());
            Task saved = taskCaptor.getValue();
            assertThat(saved.getName()).isEqualTo("Faire la vaisselle");
            assertThat(saved.getDeadline()).isEqualTo(deadline);
            assertThat(saved.getPoints()).isEqualTo(10);
            assertThat(saved.isDone()).isFalse();
            assertThat(saved.getAssigned()).isNull();
            assertThat(saved.getSharedHouse()).isSameAs(house);
            assertThat(result.assignee()).isNull();
            verify(roommateRepository, never()).findById(any());
        }

        @Test
        @DisplayName("avec un colocataire de ma coloc : il est assigné")
        void createsTaskAssignedToHouseMember() {
            SharedHouse house = house(HOUSE_ID);
            Roommate lucas = roommate(7L, house);
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(roommateRepository.findById(lucas.getId())).thenReturn(Optional.of(lucas));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse result = taskService.createTask(new CreateTaskRequest("Sortir le chien", null, lucas.getId(), 5));

            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getAssigned()).isSameAs(lucas);
            assertThat(result.assignee().id()).isEqualTo(lucas.getId());
        }

        @Test
        @DisplayName("avec un colocataire d'une autre coloc : 400 et rien n'est enregistré")
        void rejectsAssigneeFromAnotherHouse() {
            SharedHouse house = house(HOUSE_ID);
            Roommate stranger = roommate(8L, house(OTHER_HOUSE_ID));
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(roommateRepository.findById(stranger.getId())).thenReturn(Optional.of(stranger));

            CreateTaskRequest request = new CreateTaskRequest("Sortir le chien", null, stranger.getId(), 5);

            assertStatus(() -> taskService.createTask(request), HttpStatus.BAD_REQUEST);
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("avec un colocataire sans coloc : 400")
        void rejectsAssigneeWithoutHouse() {
            SharedHouse house = house(HOUSE_ID);
            Roommate homeless = roommate(9L, null);
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(roommateRepository.findById(homeless.getId())).thenReturn(Optional.of(homeless));

            CreateTaskRequest request = new CreateTaskRequest("Sortir le chien", null, homeless.getId(), 5);

            assertStatus(() -> taskService.createTask(request), HttpStatus.BAD_REQUEST);
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("avec un colocataire inexistant : 400")
        void rejectsUnknownAssignee() {
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house(HOUSE_ID));
            when(roommateRepository.findById(99L)).thenReturn(Optional.empty());

            CreateTaskRequest request = new CreateTaskRequest("Sortir le chien", null, 99L, 5);

            assertStatus(() -> taskService.createTask(request), HttpStatus.BAD_REQUEST);
            verify(taskRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("completeTask")
    class CompleteTask {

        @Test
        @DisplayName("tâche assignée : faite aujourd'hui et points donnés à l'assigné")
        void completesAssignedTaskAndGivesPointsToAssignee() {
            SharedHouse house = house(HOUSE_ID);
            Roommate lucas = roommate(7L, house);
            Task task = task(house, lucas, false, 15);
            givenTaskFound(house, task);

            TaskResponse result = taskService.completeTask(TASK_ID);

            assertThat(task.isDone()).isTrue();
            assertThat(task.getCompletionDate()).isEqualTo(LocalDate.now());
            assertThat(result.done()).isTrue();
            verify(roommateRepository).addPoints(lucas.getId(), 15);
            verify(currentUserService, never()).getCurrentRoommate();
        }

        @Test
        @DisplayName("tâche sans assigné : je deviens l'assigné et je reçois les points")
        void assignsCurrentRoommateWhenTaskIsUnassigned() {
            SharedHouse house = house(HOUSE_ID);
            Roommate me = roommate(5L, house);
            Task task = task(house, null, false, 20);
            givenTaskFound(house, task);
            when(currentUserService.getCurrentRoommate()).thenReturn(me);

            TaskResponse result = taskService.completeTask(TASK_ID);

            assertThat(task.getAssigned()).isSameAs(me);
            assertThat(result.assignee().id()).isEqualTo(me.getId());
            verify(roommateRepository).addPoints(me.getId(), 20);
        }

        @Test
        @DisplayName("tâche déjà faite : 409 et aucun point donné")
        void rejectsAlreadyDoneTask() {
            SharedHouse house = house(HOUSE_ID);
            Task task = task(house, roommate(7L, house), true, 15);
            givenTaskFound(house, task);

            assertStatus(() -> taskService.completeTask(TASK_ID), HttpStatus.CONFLICT);
            verify(roommateRepository, never()).addPoints(anyLong(), anyInt());
        }

        @Test
        @DisplayName("tâche introuvable (ou d'une autre coloc) : 404")
        void rejectsUnknownTask() {
            givenTaskNotFound();

            assertStatus(() -> taskService.completeTask(TASK_ID), HttpStatus.NOT_FOUND);
            verify(roommateRepository, never()).addPoints(anyLong(), anyInt());
        }
    }


    @Nested
    @DisplayName("reopenTask")
    class ReopenTask {

        @Test
        @DisplayName("tâche faite : remise à faire et points retirés à l'assigné")
        void reopensDoneTaskAndRemovesPoints() {
            SharedHouse house = house(HOUSE_ID);
            Roommate lucas = roommate(7L, house);
            Task task = task(house, lucas, true, 15);
            task.setCompletionDate(LocalDate.now().minusDays(1));
            givenTaskFound(house, task);

            TaskResponse result = taskService.reopenTask(TASK_ID);

            assertThat(task.isDone()).isFalse();
            assertThat(task.getCompletionDate()).isNull();
            assertThat(result.done()).isFalse();
            verify(roommateRepository).addPoints(lucas.getId(), -15);
        }

        @Test
        @DisplayName("tâche faite sans assigné (il a quitté la coloc) : remise à faire, aucun point touché")
        void reopensDoneTaskWithoutAssigneeWithoutTouchingPoints() {
            SharedHouse house = house(HOUSE_ID);
            Task task = task(house, null, true, 15);
            givenTaskFound(house, task);

            taskService.reopenTask(TASK_ID);

            assertThat(task.isDone()).isFalse();
            verify(roommateRepository, never()).addPoints(anyLong(), anyInt());
        }

        @Test
        @DisplayName("tâche pas encore faite : 409")
        void rejectsToDoTask() {
            SharedHouse house = house(HOUSE_ID);
            givenTaskFound(house, task(house, null, false, 15));

            assertStatus(() -> taskService.reopenTask(TASK_ID), HttpStatus.CONFLICT);
            verify(roommateRepository, never()).addPoints(anyLong(), anyInt());
        }
    }


    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("tâche à faire : supprimée")
        void deletesToDoTask() {
            SharedHouse house = house(HOUSE_ID);
            Task task = task(house, null, false, 5);
            givenTaskFound(house, task);

            taskService.deleteTask(TASK_ID);

            verify(taskRepository).delete(task);
        }

        @Test
        @DisplayName("tâche faite : 409 et rien n'est supprimé")
        void rejectsDoneTask() {
            SharedHouse house = house(HOUSE_ID);
            givenTaskFound(house, task(house, null, true, 5));

            assertStatus(() -> taskService.deleteTask(TASK_ID), HttpStatus.CONFLICT);
            verify(taskRepository, never()).delete(any(Task.class));
        }

        @Test
        @DisplayName("tâche introuvable : 404")
        void rejectsUnknownTask() {
            givenTaskNotFound();

            assertStatus(() -> taskService.deleteTask(TASK_ID), HttpStatus.NOT_FOUND);
            verify(taskRepository, never()).delete(any(Task.class));
        }
    }


    @Test
    @DisplayName("unassignToDoTasksOf : délègue au repository avec la coloc et le colocataire")
    void unassignToDoTasksOfDelegatesToRepository() {
        taskService.unassignToDoTasksOf(HOUSE_ID, 7L);

        verify(taskRepository).unassignToDoTasks(HOUSE_ID, 7L);
    }


    private void givenTaskFound(SharedHouse house, Task task) {
        when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
        when(taskRepository.findForUpdateByIdAndSharedHouseId(TASK_ID, house.getId())).thenReturn(Optional.of(task));
    }

    private void givenTaskNotFound() {
        when(currentUserService.getCurrentSharedHouse()).thenReturn(house(HOUSE_ID));
        when(taskRepository.findForUpdateByIdAndSharedHouseId(TASK_ID, HOUSE_ID)).thenReturn(Optional.empty());
    }

    private static void assertStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(expectedStatus);
    }

    private static SharedHouse house(Long id) {
        SharedHouse house = new SharedHouse();
        house.setId(id);
        house.setName("Coloc " + id);
        return house;
    }

    private static Roommate roommate(Long id, SharedHouse house) {
        Roommate roommate = new Roommate();
        roommate.setId(id);
        roommate.setName("Prénom" + id);
        roommate.setSurname("Nom" + id);
        roommate.setSharedHouse(house);
        return roommate;
    }

    private static Task task(SharedHouse house, Roommate assigned, boolean done, int points) {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setName("Faire la vaisselle");
        task.setSharedHouse(house);
        task.setAssigned(assigned);
        task.setDone(done);
        task.setPoints(points);
        return task;
    }
}