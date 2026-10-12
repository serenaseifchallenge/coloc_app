package com.coloc.back.task;

import com.coloc.back.dto.CreateTaskNameRequest;
import com.coloc.back.dto.TaskNameResponse;
import com.coloc.back.entity.CustomTaskName;
import com.coloc.back.entity.HiddenTaskName;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.CustomTaskNameRepository;
import com.coloc.back.repository.HiddenTaskNameRepository;
import com.coloc.back.service.CurrentUserService;
import com.coloc.back.service.TaskNameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskNameServiceTest {

    private static final Long HOUSE_ID = 1L;

    @Mock
    private HiddenTaskNameRepository hiddenTaskNameRepository;

    @Mock
    private CustomTaskNameRepository customTaskNameRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskNameService taskNameService;

    @Captor
    private ArgumentCaptor<CustomTaskName> customCaptor;

    @Captor
    private ArgumentCaptor<HiddenTaskName> hiddenCaptor;

    private SharedHouse house;

    @BeforeEach
    void setUp() {
        house = new SharedHouse();
        house.setId(HOUSE_ID);
        when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
    }


    @Test
    @DisplayName("getTaskNames : noms par défaut moins les masqués, puis les noms perso")
    void returnsVisibleDefaultsThenCustomNames() {
        when(hiddenTaskNameRepository.findNamesBySharedHouseId(HOUSE_ID))
                .thenReturn(Set.of("Faire la vaisselle", "Faire à manger"));
        when(customTaskNameRepository.findNamesBySharedHouseId(HOUSE_ID))
                .thenReturn(List.of("Sortir le chien"));

        List<String> names = taskNameService.getTaskNames();

        assertThat(names)
                .doesNotContain("Faire la vaisselle", "Faire à manger")
                .startsWith("Jeter la poubelle")
                .endsWith("Sortir le chien")
                .hasSize(13 - 2 + 1);
    }

    @Nested
    @DisplayName("addTaskName")
    class AddTaskName {

        @Test
        @DisplayName("nouveau nom perso : nettoyé et enregistré")
        void savesNewCustomName() {
            when(customTaskNameRepository.findBySharedHouseIdAndNameIgnoreCase(HOUSE_ID, "Sortir le chien"))
                    .thenReturn(Optional.empty());

            TaskNameResponse response = taskNameService.addTaskName(new CreateTaskNameRequest("  Sortir le chien "));

            verify(customTaskNameRepository).save(customCaptor.capture());
            assertThat(customCaptor.getValue().getName()).isEqualTo("Sortir le chien");
            assertThat(customCaptor.getValue().getSharedHouse()).isSameAs(house);
            assertThat(response.name()).isEqualTo("Sortir le chien");
        }

        @Test
        @DisplayName("nom perso déjà présent : 409")
        void rejectsExistingCustomName() {
            when(customTaskNameRepository.findBySharedHouseIdAndNameIgnoreCase(HOUSE_ID, "Sortir le chien"))
                    .thenReturn(Optional.of(new CustomTaskName()));

            assertStatus(() -> taskNameService.addTaskName(new CreateTaskNameRequest("Sortir le chien")),
                    HttpStatus.CONFLICT);
            verify(customTaskNameRepository, never()).save(any());
        }

        @Test
        @DisplayName("nom par défaut masqué (casse différente) : il est réaffiché avec son orthographe d'origine")
        void restoresHiddenDefaultName() {
            HiddenTaskName hidden = new HiddenTaskName();
            when(hiddenTaskNameRepository.findBySharedHouseIdAndName(HOUSE_ID, "Faire la vaisselle"))
                    .thenReturn(Optional.of(hidden));

            TaskNameResponse response = taskNameService.addTaskName(new CreateTaskNameRequest("faire LA vaisselle"));

            verify(hiddenTaskNameRepository).delete(hidden);
            assertThat(response.name()).isEqualTo("Faire la vaisselle");
            verify(customTaskNameRepository, never()).save(any());
        }

        @Test
        @DisplayName("nom par défaut déjà visible : 409")
        void rejectsVisibleDefaultName() {
            when(hiddenTaskNameRepository.findBySharedHouseIdAndName(HOUSE_ID, "Faire la vaisselle"))
                    .thenReturn(Optional.empty());

            assertStatus(() -> taskNameService.addTaskName(new CreateTaskNameRequest("Faire la vaisselle")),
                    HttpStatus.CONFLICT);
            verify(hiddenTaskNameRepository, never()).delete(any());
        }
    }


    @Nested
    @DisplayName("deleteTaskName")
    class DeleteTaskName {

        @Test
        @DisplayName("nom perso : supprimé de la base")
        void deletesCustomName() {
            CustomTaskName custom = new CustomTaskName();
            when(customTaskNameRepository.findBySharedHouseIdAndNameIgnoreCase(HOUSE_ID, "Sortir le chien"))
                    .thenReturn(Optional.of(custom));

            taskNameService.deleteTaskName("Sortir le chien");

            verify(customTaskNameRepository).delete(custom);
            verify(hiddenTaskNameRepository, never()).save(any());
        }

        @Test
        @DisplayName("nom par défaut : masqué pour ma coloc")
        void hidesDefaultName() {
            when(customTaskNameRepository.findBySharedHouseIdAndNameIgnoreCase(HOUSE_ID, "Faire la vaisselle"))
                    .thenReturn(Optional.empty());
            when(hiddenTaskNameRepository.existsBySharedHouseIdAndName(HOUSE_ID, "Faire la vaisselle"))
                    .thenReturn(false);

            taskNameService.deleteTaskName("Faire la vaisselle");

            verify(hiddenTaskNameRepository).save(hiddenCaptor.capture());
            assertThat(hiddenCaptor.getValue().getName()).isEqualTo("Faire la vaisselle");
            assertThat(hiddenCaptor.getValue().getSharedHouse()).isSameAs(house);
        }

        @Test
        @DisplayName("nom par défaut déjà masqué : rien ne se passe (pas de doublon)")
        void ignoresAlreadyHiddenDefaultName() {
            when(customTaskNameRepository.findBySharedHouseIdAndNameIgnoreCase(HOUSE_ID, "Faire la vaisselle"))
                    .thenReturn(Optional.empty());
            when(hiddenTaskNameRepository.existsBySharedHouseIdAndName(HOUSE_ID, "Faire la vaisselle"))
                    .thenReturn(true);

            taskNameService.deleteTaskName("Faire la vaisselle");

            verify(hiddenTaskNameRepository, never()).save(any());
        }

        @Test
        @DisplayName("nom inconnu : 404")
        void rejectsUnknownName() {
            when(customTaskNameRepository.findBySharedHouseIdAndNameIgnoreCase(HOUSE_ID, "Repeindre le salon"))
                    .thenReturn(Optional.empty());

            assertStatus(() -> taskNameService.deleteTaskName("Repeindre le salon"), HttpStatus.NOT_FOUND);
            verify(hiddenTaskNameRepository, never()).save(any());
        }
    }


    private static void assertStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(expectedStatus);
    }
}