package com.coloc.back.auth;

import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.service.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    private static final Long ME_ID = 5L;

    @Mock
    private RoommateRepository roommateRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("personne de connecté : 401")
    void rejectsWhenNobodyIsLoggedIn() {
        assertStatus(() -> currentUserService.getCurrentRoommateId(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("connecté via le token : renvoie l'id mis par le filtre JWT")
    void returnsIdFromSecurityContext() {
        loginAs(ME_ID);

        assertThat(currentUserService.getCurrentRoommateId()).isEqualTo(ME_ID);
    }

    @Test
    @DisplayName("compte introuvable en base (supprimé entre-temps) : 401")
    void rejectsWhenRoommateNoLongerExists() {
        loginAs(ME_ID);
        when(roommateRepository.findById(ME_ID)).thenReturn(Optional.empty());

        assertStatus(() -> currentUserService.getCurrentRoommate(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("pas encore de coloc : 403")
    void rejectsWhenRoommateHasNoHouse() {
        loginAs(ME_ID);
        when(roommateRepository.findById(ME_ID)).thenReturn(Optional.of(roommate(null)));

        assertStatus(() -> currentUserService.getCurrentSharedHouse(), HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("avec une coloc : renvoie la coloc du colocataire connecté")
    void returnsHouseOfCurrentRoommate() {
        SharedHouse house = new SharedHouse();
        house.setId(1L);
        loginAs(ME_ID);
        when(roommateRepository.findById(ME_ID)).thenReturn(Optional.of(roommate(house)));

        assertThat(currentUserService.getCurrentSharedHouse()).isSameAs(house);
    }


    private static void loginAs(Long roommateId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(roommateId, null, List.of()));
    }

    private static void assertStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(expectedStatus);
    }

    private static Roommate roommate(SharedHouse house) {
        Roommate roommate = new Roommate();
        roommate.setId(ME_ID);
        roommate.setSharedHouse(house);
        return roommate;
    }
}
