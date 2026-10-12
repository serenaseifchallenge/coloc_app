package com.coloc.back.auth;

import com.coloc.back.dto.AuthResponse;
import com.coloc.back.dto.LoginRequest;
import com.coloc.back.dto.RegisterRequest;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.security.JwtService;
import com.coloc.back.service.AuthService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long ME_ID = 5L;
    private static final LocalDate BIRTHDAY = LocalDate.of(2003, 4, 12);

    @Mock
    private RoommateRepository roommateRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<Roommate> roommateCaptor;


    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("crée le compte : champs nettoyés, email en minuscules, mot de passe haché, 0 point, token renvoyé")
        void createsAccountAndReturnsToken() {
            when(roommateRepository.existsByEmail("emma@mail.fr")).thenReturn(false);
            when(passwordEncoder.encode("motdepasse")).thenReturn("hash");
            when(roommateRepository.save(any(Roommate.class))).thenAnswer(invocation -> {
                Roommate saved = invocation.getArgument(0);
                saved.setId(ME_ID);
                return saved;
            });
            when(jwtService.generateToken(ME_ID)).thenReturn("token");

            AuthResponse result = authService.register(
                    new RegisterRequest("  Emma ", " Martin  ", "  Emma@Mail.FR ", "motdepasse", BIRTHDAY));

            verify(roommateRepository).save(roommateCaptor.capture());
            Roommate saved = roommateCaptor.getValue();
            assertThat(saved.getName()).isEqualTo("Emma");
            assertThat(saved.getSurname()).isEqualTo("Martin");
            assertThat(saved.getEmail()).isEqualTo("emma@mail.fr");
            assertThat(saved.getPassword()).isEqualTo("hash");
            assertThat(saved.getPoints()).isZero();
            assertThat(saved.getBirthday()).isEqualTo(BIRTHDAY.atStartOfDay());
            assertThat(saved.getSharedHouse()).isNull();

            assertThat(result.token()).isEqualTo("token");
            assertThat(result.roommate().id()).isEqualTo(ME_ID);
            assertThat(result.roommate().email()).isEqualTo("emma@mail.fr");
        }

        @Test
        @DisplayName("email déjà utilisé (même avec une autre casse) : 409 et rien n'est enregistré")
        void rejectsAlreadyUsedEmail() {
            when(roommateRepository.existsByEmail("emma@mail.fr")).thenReturn(true);

            assertStatus(() -> authService.register(
                    new RegisterRequest("Emma", "Martin", "EMMA@mail.fr", "motdepasse", BIRTHDAY)),
                    HttpStatus.CONFLICT);

            verify(roommateRepository, never()).save(any());
            verify(jwtService, never()).generateToken(anyLong());
        }
    }


    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("bons identifiants : email nettoyé et mis en minuscules, token renvoyé")
        void returnsTokenWhenCredentialsAreValid() {
            Roommate me = roommate();
            when(roommateRepository.findByEmail("emma@mail.fr")).thenReturn(Optional.of(me));
            when(passwordEncoder.matches("motdepasse", "hash")).thenReturn(true);
            when(jwtService.generateToken(ME_ID)).thenReturn("token");

            AuthResponse result = authService.login(new LoginRequest(" Emma@Mail.fr ", "motdepasse"));

            assertThat(result.token()).isEqualTo("token");
            assertThat(result.roommate().id()).isEqualTo(ME_ID);
        }

        @Test
        @DisplayName("mauvais mot de passe : 401 et aucun token")
        void rejectsWrongPassword() {
            when(roommateRepository.findByEmail("emma@mail.fr")).thenReturn(Optional.of(roommate()));
            when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);

            assertStatus(() -> authService.login(new LoginRequest("emma@mail.fr", "mauvais")),
                    HttpStatus.UNAUTHORIZED);

            verify(jwtService, never()).generateToken(anyLong());
        }

        @Test
        @DisplayName("email inconnu : 401, même message que pour un mauvais mot de passe")
        void rejectsUnknownEmail() {
            when(roommateRepository.findByEmail("inconnu@mail.fr")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("inconnu@mail.fr", "motdepasse")))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email ou mot de passe incorrect");

            verify(passwordEncoder, never()).matches(any(), any());
        }
    }


    private static void assertStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(expectedStatus);
    }

    private static Roommate roommate() {
        Roommate roommate = new Roommate();
        roommate.setId(ME_ID);
        roommate.setName("Emma");
        roommate.setSurname("Martin");
        roommate.setEmail("emma@mail.fr");
        roommate.setPassword("hash");
        roommate.setPoints(0);
        roommate.setBirthday(BIRTHDAY.atStartOfDay());
        return roommate;
    }
}
