package com.coloc.back.roommate;

import com.coloc.back.dto.RoommateResponse;
import com.coloc.back.dto.RoommateSummaryResponse;
import com.coloc.back.dto.UpdateAvatarRequest;
import com.coloc.back.dto.UpdateCredentialsRequest;
import com.coloc.back.dto.UpdateProfileRequest;
import com.coloc.back.entity.Note;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.EventRepository;
import com.coloc.back.repository.NoteRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.service.CurrentUserService;
import com.coloc.back.service.RoommateService;
import com.coloc.back.service.SharedHouseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoommateServiceTest {

    private static final Long HOUSE_ID = 1L;
    private static final Long ME_ID = 5L;

    @Mock
    private RoommateRepository roommateRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SharedHouseService sharedHouseService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RoommateService roommateService;


    @Nested
    @DisplayName("getMe / updateMe")
    class Profile {

        @Test
        @DisplayName("getMe : mon profil, avec l'id de ma coloc")
        void returnsMyProfile() {
            when(currentUserService.getCurrentRoommate()).thenReturn(roommate(ME_ID, "Emma", house()));

            RoommateResponse result = roommateService.getMe();

            assertThat(result.id()).isEqualTo(ME_ID);
            assertThat(result.email()).isEqualTo("emma@mail.fr");
            assertThat(result.sharedHouseId()).isEqualTo(HOUSE_ID);
        }

        @Test
        @DisplayName("updateMe : prénom et nom nettoyés, date de naissance mise à jour")
        void updatesNameAndBirthday() {
            Roommate me = givenMe();
            LocalDate birthday = LocalDate.of(2001, 9, 30);
            when(roommateRepository.save(me)).thenReturn(me);

            RoommateResponse result = roommateService.updateMe(new UpdateProfileRequest("  Emmy ", " Durand  ", birthday));

            assertThat(me.getName()).isEqualTo("Emmy");
            assertThat(me.getSurname()).isEqualTo("Durand");
            assertThat(me.getBirthday()).isEqualTo(birthday.atStartOfDay());
            assertThat(result.birthday()).isEqualTo(birthday);
        }
    }


    @Nested
    @DisplayName("updateCredentials")
    class UpdateCredentials {

        @Test
        @DisplayName("nouvel email libre : enregistré en minuscules, mot de passe inchangé si non fourni")
        void changesEmailOnly() {
            Roommate me = givenMe();
            when(roommateRepository.existsByEmail("emma.durand@mail.fr")).thenReturn(false);
            when(roommateRepository.save(me)).thenReturn(me);

            roommateService.updateCredentials(new UpdateCredentialsRequest(" Emma.Durand@Mail.fr ", null));

            assertThat(me.getEmail()).isEqualTo("emma.durand@mail.fr");
            assertThat(me.getPassword()).isEqualTo("ancien-hash");
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("email déjà utilisé par quelqu'un d'autre : 409 et rien n'est enregistré")
        void rejectsEmailUsedByAnotherAccount() {
            Roommate me = givenMe();
            when(roommateRepository.existsByEmail("lucas@mail.fr")).thenReturn(true);

            assertStatus(() -> roommateService.updateCredentials(new UpdateCredentialsRequest("lucas@mail.fr", null)),
                    HttpStatus.CONFLICT);

            assertThat(me.getEmail()).isEqualTo("emma@mail.fr");
            verify(roommateRepository, never()).save(any());
        }

        @Test
        @DisplayName("même email qu'avant + nouveau mot de passe : pas de 409, mot de passe haché")
        void keepsSameEmailAndChangesPassword() {
            Roommate me = givenMe();
            when(passwordEncoder.encode("nouveau-mdp")).thenReturn("nouveau-hash");
            when(roommateRepository.save(me)).thenReturn(me);

            roommateService.updateCredentials(new UpdateCredentialsRequest("EMMA@mail.fr", "nouveau-mdp"));

            verify(roommateRepository, never()).existsByEmail(anyString());
            assertThat(me.getPassword()).isEqualTo("nouveau-hash");
        }

        @Test
        @DisplayName("mot de passe vide : il n'est pas changé")
        void ignoresBlankPassword() {
            Roommate me = givenMe();
            when(roommateRepository.save(me)).thenReturn(me);

            roommateService.updateCredentials(new UpdateCredentialsRequest("emma@mail.fr", "   "));

            assertThat(me.getPassword()).isEqualTo("ancien-hash");
            verify(passwordEncoder, never()).encode(anyString());
        }
    }


    @Nested
    @DisplayName("updateAvatar")
    class UpdateAvatar {

        @Test
        @DisplayName("couleur mise en minuscules et photo enregistrée")
        void savesColorAndImage() {
            Roommate me = givenMe();
            when(roommateRepository.save(me)).thenReturn(me);

            RoommateResponse result = roommateService.updateAvatar(
                    new UpdateAvatarRequest("#6C63FF", "data:image/jpeg;base64,AAAA"));

            assertThat(me.getAvatarColor()).isEqualTo("#6c63ff");
            assertThat(me.getAvatarImage()).isEqualTo("data:image/jpeg;base64,AAAA");
            assertThat(result.avatarColor()).isEqualTo("#6c63ff");
        }

        @Test
        @DisplayName("null partout : retour à la couleur par défaut et aux initiales")
        void resetsAvatar() {
            Roommate me = givenMe();
            me.setAvatarColor("#e8261e");
            me.setAvatarImage("data:image/png;base64,BBBB");
            when(roommateRepository.save(me)).thenReturn(me);

            roommateService.updateAvatar(new UpdateAvatarRequest(null, null));

            assertThat(me.getAvatarColor()).isNull();
            assertThat(me.getAvatarImage()).isNull();
        }
    }


    @Nested
    @DisplayName("deleteMe")
    class DeleteMe {

        @Test
        @DisplayName("dans une coloc : mes notes sont supprimées, je quitte la coloc, puis le compte est supprimé")
        void leavesHouseThenDeletesAccount() {
            Roommate me = roommate(ME_ID, "Emma", house());
            List<Note> myNotes = List.of(new Note());
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(noteRepository.findByCreator_Id(ME_ID)).thenReturn(myNotes);

            roommateService.deleteMe();

            InOrder order = inOrder(noteRepository, sharedHouseService, roommateRepository);
            order.verify(noteRepository).deleteAll(myNotes);
            order.verify(sharedHouseService).leave();
            order.verify(roommateRepository).delete(me);
            order.verify(roommateRepository).flush();
        }

        @Test
        @DisplayName("sans coloc : le compte est supprimé directement")
        void deletesAccountWithoutHouse() {
            Roommate me = givenMe();
            when(noteRepository.findByCreator_Id(ME_ID)).thenReturn(List.of());

            roommateService.deleteMe();

            verify(sharedHouseService, never()).leave();
            verify(roommateRepository).delete(me);
        }

        @Test
        @DisplayName("départ refusé (solde ou cagnotte) : l'erreur remonte et le compte n'est pas supprimé")
        void keepsAccountWhenLeaveIsRefused() {
            Roommate me = roommate(ME_ID, "Emma", house());
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(noteRepository.findByCreator_Id(ME_ID)).thenReturn(List.of());
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Avant de quitter la colocation, ..."))
                    .when(sharedHouseService).leave();

            assertStatus(() -> roommateService.deleteMe(), HttpStatus.CONFLICT);

            verify(roommateRepository, never()).delete(any());
        }

        @Test
        @DisplayName("compte encore lié à des données en base : 409 avec un message clair")
        void returnsConflictWhenAccountIsStillReferenced() {
            givenMe();
            when(noteRepository.findByCreator_Id(ME_ID)).thenReturn(List.of());
            doThrow(new DataIntegrityViolationException("clé étrangère")).when(roommateRepository).flush();

            assertThatThrownBy(() -> roommateService.deleteMe())
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Impossible de supprimer ce compte");
        }
    }


    @Test
    @DisplayName("getRoommatesOfCurrentSharedHouse : membres de ma coloc triés par prénom")
    void returnsRoommatesSortedByName() {
        SharedHouse house = house();
        when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
        when(roommateRepository.findBySharedHouseId(HOUSE_ID)).thenReturn(List.of(
                roommate(7L, "Lucas", house), roommate(ME_ID, "Emma", house), roommate(9L, "Inès", house)));

        List<RoommateSummaryResponse> result = roommateService.getRoommatesOfCurrentSharedHouse();

        assertThat(result).extracting(RoommateSummaryResponse::name).containsExactly("Emma", "Inès", "Lucas");
    }


    /** Moi, connecté(e), sans coloc. */
    private Roommate givenMe() {
        Roommate me = roommate(ME_ID, "Emma", null);
        when(currentUserService.getCurrentRoommate()).thenReturn(me);
        return me;
    }

    private static void assertStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(expectedStatus);
    }

    private static SharedHouse house() {
        SharedHouse house = new SharedHouse();
        house.setId(HOUSE_ID);
        house.setName("Coloc " + HOUSE_ID);
        return house;
    }

    private static Roommate roommate(Long id, String name, SharedHouse house) {
        Roommate roommate = new Roommate();
        roommate.setId(id);
        roommate.setName(name);
        roommate.setSurname("Martin");
        roommate.setEmail(name.toLowerCase() + "@mail.fr");
        roommate.setPassword("ancien-hash");
        roommate.setPoints(0);
        roommate.setBirthday(LocalDate.of(2002, 1, 1).atStartOfDay());
        roommate.setSharedHouse(house);
        return roommate;
    }
}
