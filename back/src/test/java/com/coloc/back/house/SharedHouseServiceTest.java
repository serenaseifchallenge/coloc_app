package com.coloc.back.house;

import com.coloc.back.dto.JoinSharedHouseRequest;
import com.coloc.back.dto.RoommateResponse;
import com.coloc.back.dto.SharedHouseRequest;
import com.coloc.back.dto.SharedHouseResponse;
import com.coloc.back.entity.Note;
import com.coloc.back.entity.Pot;
import com.coloc.back.entity.PotPayment;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.EventRepository;
import com.coloc.back.repository.NoteRepository;
import com.coloc.back.repository.PotPaymentRepository;
import com.coloc.back.repository.PotRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.repository.SharedHouseRepository;
import com.coloc.back.service.ArticleService;
import com.coloc.back.service.BalanceService;
import com.coloc.back.service.CurrentUserService;
import com.coloc.back.service.SharedHouseService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedHouseServiceTest {

    private static final Long HOUSE_ID = 1L;
    private static final Long ME_ID = 5L;
    private static final Long LUCAS_ID = 8L;
    private static final Long INES_ID = 9L;

    @Mock
    private SharedHouseRepository sharedHouseRepository;

    @Mock
    private RoommateRepository roommateRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TaskService taskService;

    @Mock
    private ArticleService articleService;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private BalanceService balanceService;

    @Mock
    private PotRepository potRepository;

    @Mock
    private PotPaymentRepository potPaymentRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private SharedHouseService sharedHouseService;

    @Captor
    private ArgumentCaptor<SharedHouse> houseCaptor;


    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("crée la coloc : champs nettoyés, code de 8 caractères sans 0/O/1/I, je suis membre")
        void createsHouseAndJoinsIt() {
            Roommate me = roommate(ME_ID, null);
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(sharedHouseRepository.existsByInvitationCode(anyString())).thenReturn(false);
            when(roommateRepository.findBySharedHouseId(any())).thenReturn(List.of(me));

            SharedHouseResponse result = sharedHouseService.create(
                    new SharedHouseRequest("  La Coloc  ", " 12 rue des Lilas ", " Super coloc "));

            verify(sharedHouseRepository).save(houseCaptor.capture());
            SharedHouse saved = houseCaptor.getValue();
            assertThat(saved.getName()).isEqualTo("La Coloc");
            assertThat(saved.getAddress()).isEqualTo("12 rue des Lilas");
            assertThat(saved.getDescription()).isEqualTo("Super coloc");
            assertThat(saved.getInvitationCode()).matches("[A-HJ-NP-Z2-9]{8}");

            assertThat(me.getSharedHouse()).isSameAs(saved);
            verify(roommateRepository).save(me);
            assertThat(result.members()).extracting(RoommateResponse::id).containsExactly(ME_ID);
        }

        @Test
        @DisplayName("code déjà pris : on en tire un autre")
        void regeneratesCodeWhenAlreadyUsed() {
            Roommate me = roommate(ME_ID, null);
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(sharedHouseRepository.existsByInvitationCode(anyString())).thenReturn(true, false);
            when(roommateRepository.findBySharedHouseId(any())).thenReturn(List.of(me));

            sharedHouseService.create(new SharedHouseRequest("La Coloc", "12 rue des Lilas", "Super coloc"));

            verify(sharedHouseRepository, times(2)).existsByInvitationCode(anyString());
        }

        @Test
        @DisplayName("déjà dans une coloc : 409 et rien n'est créé")
        void rejectsWhenAlreadyInHouse() {
            when(currentUserService.getCurrentRoommate()).thenReturn(roommate(ME_ID, house()));

            assertStatus(() -> sharedHouseService.create(
                    new SharedHouseRequest("La Coloc", "12 rue des Lilas", "Super coloc")), HttpStatus.CONFLICT);

            verify(sharedHouseRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("join")
    class Join {

        @Test
        @DisplayName("code nettoyé et mis en majuscules : je rejoins la coloc")
        void joinsHouseWithNormalizedCode() {
            SharedHouse house = house();
            Roommate me = roommate(ME_ID, null);
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(sharedHouseRepository.findByInvitationCode("ABCD2345")).thenReturn(Optional.of(house));
            when(roommateRepository.findBySharedHouseId(HOUSE_ID)).thenReturn(List.of(me));

            SharedHouseResponse result = sharedHouseService.join(new JoinSharedHouseRequest("  abcd2345 "));

            assertThat(me.getSharedHouse()).isSameAs(house);
            verify(roommateRepository).save(me);
            assertThat(result.id()).isEqualTo(HOUSE_ID);
        }

        @Test
        @DisplayName("code inconnu : 404")
        void rejectsUnknownCode() {
            Roommate me = roommate(ME_ID, null);
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(sharedHouseRepository.findByInvitationCode("INCONNU1")).thenReturn(Optional.empty());

            assertStatus(() -> sharedHouseService.join(new JoinSharedHouseRequest("inconnu1")), HttpStatus.NOT_FOUND);

            assertThat(me.getSharedHouse()).isNull();
            verify(roommateRepository, never()).save(any());
        }

        @Test
        @DisplayName("déjà dans une coloc : 409, le code n'est même pas cherché")
        void rejectsWhenAlreadyInHouse() {
            when(currentUserService.getCurrentRoommate()).thenReturn(roommate(ME_ID, house()));

            assertStatus(() -> sharedHouseService.join(new JoinSharedHouseRequest("ABCD2345")), HttpStatus.CONFLICT);

            verify(sharedHouseRepository, never()).findByInvitationCode(anyString());
        }
    }


    @Nested
    @DisplayName("getMine / update")
    class GetMineAndUpdate {

        @Test
        @DisplayName("getMine : ma coloc avec ses membres")
        void returnsMyHouseWithMembers() {
            SharedHouse house = house();
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(roommateRepository.findBySharedHouseId(HOUSE_ID))
                    .thenReturn(List.of(roommate(ME_ID, house), roommate(LUCAS_ID, house)));

            SharedHouseResponse result = sharedHouseService.getMine();

            assertThat(result.name()).isEqualTo("Coloc 1");
            assertThat(result.invitationCode()).isEqualTo("ABCD2345");
            assertThat(result.members()).extracting(RoommateResponse::id).containsExactly(ME_ID, LUCAS_ID);
        }

        @Test
        @DisplayName("update : nom, adresse et description nettoyés puis enregistrés")
        void updatesHouseInformation() {
            SharedHouse house = house();
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(sharedHouseRepository.save(house)).thenReturn(house);
            when(roommateRepository.findBySharedHouseId(HOUSE_ID)).thenReturn(List.of());

            SharedHouseResponse result = sharedHouseService.update(
                    new SharedHouseRequest(" Nouvelle coloc ", " 3 avenue Foch ", " Calme "));

            assertThat(house.getName()).isEqualTo("Nouvelle coloc");
            assertThat(house.getAddress()).isEqualTo("3 avenue Foch");
            assertThat(house.getDescription()).isEqualTo("Calme");
            assertThat(house.getInvitationCode()).isEqualTo("ABCD2345");
            assertThat(result.name()).isEqualTo("Nouvelle coloc");
        }
    }


    @Nested
    @DisplayName("leave")
    class Leave {

        @Test
        @DisplayName("pas de coloc : 409")
        void rejectsWhenNotInHouse() {
            when(currentUserService.getCurrentRoommate()).thenReturn(roommate(ME_ID, null));

            assertStatus(() -> sharedHouseService.leave(), HttpStatus.CONFLICT);

            verify(roommateRepository, never()).save(any());
        }

        @Test
        @DisplayName("dernier membre : pas de vérification, mes notes sont supprimées et la coloc aussi")
        void lastMemberLeavesAndHouseIsDeleted() {
            SharedHouse house = house();
            Roommate me = roommate(ME_ID, house);
            List<Note> myNotes = List.of(new Note());
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(noteRepository.findByCreator_Id(ME_ID)).thenReturn(myNotes);
            when(roommateRepository.findBySharedHouseId(HOUSE_ID))
                    .thenReturn(List.of(me))
                    .thenReturn(List.of());

            sharedHouseService.leave();

            verify(noteRepository).deleteAll(myNotes);
            verify(balanceService, never()).computeBalances(anyLong());
            verify(potRepository, never()).findBySharedHouseIdOrderByIdDesc(anyLong());
            verify(taskService).unassignToDoTasksOf(HOUSE_ID, ME_ID);
            verify(articleService).deletePersonalArticlesOf(HOUSE_ID, ME_ID);
            assertThat(me.getSharedHouse()).isNull();
            verify(roommateRepository).save(me);
            verify(sharedHouseRepository).delete(house);
        }

        @Test
        @DisplayName("je dois de l'argent : 409 avec le montant, je reste dans la coloc")
        void rejectsWhenIOweMoney() {
            Roommate me = givenTwoMembersAndMyBalance(new BigDecimal("-12.50"));
            givenMyPaymentsInOpenPots();

            assertThatThrownBy(() -> sharedHouseService.leave())
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("remboursez les 12,50 € que vous devez");

            assertThat(me.getSharedHouse()).isNotNull();
            verify(roommateRepository, never()).save(any());
            verify(taskService, never()).unassignToDoTasksOf(anyLong(), anyLong());
        }

        @Test
        @DisplayName("on me doit de l'argent : 409, il faut se faire rembourser avant de partir")
        void rejectsWhenOthersOweMeMoney() {
            givenTwoMembersAndMyBalance(new BigDecimal("8.00"));
            givenMyPaymentsInOpenPots();

            assertThatThrownBy(() -> sharedHouseService.leave())
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("faites-vous rembourser les 8,00 € qu'on vous doit");
        }

        @Test
        @DisplayName("argent dans une cagnotte en cours : 409 avec le nom et le total versé")
        void rejectsWhenMoneyIsInOpenPot() {
            givenTwoMembersAndMyBalance(BigDecimal.ZERO);
            Pot gift = pot(20L, "Cadeau Léa", LUCAS_ID, false, ME_ID, LUCAS_ID);
            givenMyPaymentsInOpenPots(payment(gift, "10.00"), payment(gift, "5.50"));

            assertThatThrownBy(() -> sharedHouseService.leave())
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("récupérez votre argent de la cagnotte « Cadeau Léa » (15,50 €)");
        }

        @Test
        @DisplayName("dette + cagnotte : les deux problèmes sont dans le même message")
        void listsEveryProblemInOneMessage() {
            givenTwoMembersAndMyBalance(new BigDecimal("-3.00"));
            Pot trip = pot(21L, "Week-end", LUCAS_ID, false, ME_ID, LUCAS_ID);
            Pot gift = pot(22L, "Cadeau Léa", LUCAS_ID, false, ME_ID, LUCAS_ID);
            givenMyPaymentsInOpenPots(payment(trip, "10.00"), payment(gift, "4.00"));

            assertThatThrownBy(() -> sharedHouseService.leave())
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("remboursez les 3,00 € que vous devez et récupérez votre argent des cagnottes")
                    .hasMessageContaining("« Week-end » (10,00 €), « Cadeau Léa » (4,00 €)");
        }

        @Test
        @DisplayName("quitte : je sors des cagnottes en cours et mes cagnottes passent au plus petit id restant")
        void leavesAndHandsOverPots() {
            SharedHouse house = house();
            Roommate me = roommate(ME_ID, house);
            Roommate lucas = roommate(LUCAS_ID, house);
            Roommate ines = roommate(INES_ID, house);
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(noteRepository.findByCreator_Id(ME_ID)).thenReturn(List.of());
            when(roommateRepository.findBySharedHouseId(HOUSE_ID))
                    .thenReturn(List.of(me, lucas, ines))
                    .thenReturn(List.of(me, lucas, ines))
                    .thenReturn(List.of(lucas, ines));
            when(balanceService.computeBalances(HOUSE_ID))
                    .thenReturn(Map.of(ME_ID, BigDecimal.ZERO, LUCAS_ID, BigDecimal.ZERO, INES_ID, BigDecimal.ZERO));
            when(potPaymentRepository.findByRoommateIdAndPotSharedHouseIdAndPotPurchasedFalse(ME_ID, HOUSE_ID))
                    .thenReturn(List.of());

            Pot myOpenPot = pot(30L, "Barbecue", ME_ID, false, ME_ID, INES_ID);
            Pot myPurchasedPot = pot(31L, "Aspirateur", ME_ID, true, ME_ID);
            Pot lucasOpenPot = pot(32L, "Cadeau Léa", LUCAS_ID, false, ME_ID, LUCAS_ID);
            when(potRepository.findBySharedHouseIdOrderByIdDesc(HOUSE_ID))
                    .thenReturn(List.of(myOpenPot, myPurchasedPot, lucasOpenPot));

            sharedHouseService.leave();

            // Cagnotte en cours que j'ai créée : je n'y participe plus, elle passe à l'autre participant
            assertThat(myOpenPot.getParticipantIds()).containsExactly(INES_ID);
            assertThat(myOpenPot.getCreatedById()).isEqualTo(INES_ID);
            // Cagnotte déjà achetée : l'historique ne bouge pas, mais il faut un nouveau créateur
            assertThat(myPurchasedPot.getParticipantIds()).containsExactly(ME_ID);
            assertThat(myPurchasedPot.getCreatedById()).isEqualTo(LUCAS_ID);
            // Cagnotte d'un autre : je suis juste retiré des participants
            assertThat(lucasOpenPot.getParticipantIds()).containsExactly(LUCAS_ID);
            assertThat(lucasOpenPot.getCreatedById()).isEqualTo(LUCAS_ID);

            verify(taskService).unassignToDoTasksOf(HOUSE_ID, ME_ID);
            verify(articleService).deletePersonalArticlesOf(HOUSE_ID, ME_ID);
            assertThat(me.getSharedHouse()).isNull();
            verify(roommateRepository).save(me);
            verify(sharedHouseRepository, never()).delete(any());
        }
    }


    /** Moi + Lucas dans la coloc, avec mon solde donné. */
    private Roommate givenTwoMembersAndMyBalance(BigDecimal myBalance) {
        SharedHouse house = house();
        Roommate me = roommate(ME_ID, house);
        when(currentUserService.getCurrentRoommate()).thenReturn(me);
        when(noteRepository.findByCreator_Id(ME_ID)).thenReturn(List.of());
        when(roommateRepository.findBySharedHouseId(HOUSE_ID)).thenReturn(List.of(me, roommate(LUCAS_ID, house)));
        when(balanceService.computeBalances(HOUSE_ID)).thenReturn(Map.of(ME_ID, myBalance, LUCAS_ID, myBalance.negate()));
        return me;
    }

    private void givenMyPaymentsInOpenPots(PotPayment... payments) {
        when(potPaymentRepository.findByRoommateIdAndPotSharedHouseIdAndPotPurchasedFalse(ME_ID, HOUSE_ID))
                .thenReturn(List.of(payments));
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
        house.setAddress("12 rue des Lilas");
        house.setDescription("Super coloc");
        house.setInvitationCode("ABCD2345");
        return house;
    }

    private static Roommate roommate(Long id, SharedHouse house) {
        Roommate roommate = new Roommate();
        roommate.setId(id);
        roommate.setName("Prénom" + id);
        roommate.setSurname("Nom" + id);
        roommate.setEmail("coloc" + id + "@mail.fr");
        roommate.setPoints(0);
        roommate.setBirthday(LocalDate.of(2002, 1, 1).atStartOfDay());
        roommate.setSharedHouse(house);
        return roommate;
    }

    private static Pot pot(Long id, String name, Long createdById, boolean purchased, Long... participantIds) {
        Pot pot = new Pot();
        pot.setId(id);
        pot.setSharedHouseId(HOUSE_ID);
        pot.setName(name);
        pot.setCreatedById(createdById);
        pot.setPurchased(purchased);
        pot.setTargetAmount(new BigDecimal("100.00"));
        pot.getParticipantIds().addAll(List.of(participantIds));
        return pot;
    }

    private static PotPayment payment(Pot pot, String amount) {
        PotPayment payment = new PotPayment();
        payment.setPot(pot);
        payment.setAmount(new BigDecimal(amount));
        return payment;
    }
}
