package com.coloc.back.service;

import com.coloc.back.dto.JoinSharedHouseRequest;
import com.coloc.back.dto.SharedHouseRequest;
import com.coloc.back.dto.SharedHouseResponse;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.repository.SharedHouseRepository;
import com.coloc.back.repository.NoteRepository;
import com.coloc.back.entity.Pot;
import com.coloc.back.entity.PotPayment;
import com.coloc.back.repository.PotPaymentRepository;
import com.coloc.back.repository.PotRepository;
import com.coloc.back.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SharedHouseService {

    // Pas de 0/O ni 1/I pour éviter les confusions quand on recopie le code
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SharedHouseRepository sharedHouseRepository;
    private final RoommateRepository roommateRepository;
    private final CurrentUserService currentUserService;
    private final TaskService taskService;
    private final ArticleService articleService;
    private final NoteRepository noteRepository;
    private final BalanceService balanceService;
    private final PotRepository potRepository;
    private final PotPaymentRepository potPaymentRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public SharedHouseResponse getMine() {
        return toResponse(currentUserService.getCurrentSharedHouse());
    }

    @Transactional
    public SharedHouseResponse create(SharedHouseRequest request) {
        Roommate me = currentUserService.getCurrentRoommate();
        checkHasNoHouse(me);

        SharedHouse house = new SharedHouse();
        house.setName(request.name().trim());
        house.setAddress(request.address().trim());
        house.setDescription(request.description().trim());
        house.setInvitationCode(generateUniqueCode());
        sharedHouseRepository.save(house);

        me.setSharedHouse(house);
        roommateRepository.save(me);

        return toResponse(house);
    }

    @Transactional
    public SharedHouseResponse update(SharedHouseRequest request) {
        SharedHouse house = currentUserService.getCurrentSharedHouse();
        house.setName(request.name().trim());
        house.setAddress(request.address().trim());
        house.setDescription(request.description().trim());
        return toResponse(sharedHouseRepository.save(house));
    }

    @Transactional
    public SharedHouseResponse join(JoinSharedHouseRequest request) {
        Roommate me = currentUserService.getCurrentRoommate();
        checkHasNoHouse(me);

        String code = request.invitationCode().trim().toUpperCase();
        SharedHouse house = sharedHouseRepository.findByInvitationCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucune colocation ne correspond à ce code"));

        me.setSharedHouse(house);
        roommateRepository.save(me);

        return toResponse(house);
    }

    @Transactional
    public void leave() {
        Roommate me = currentUserService.getCurrentRoommate();
        SharedHouse house = me.getSharedHouse();
        if (house == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous n'êtes dans aucune colocation");
        }
        // Supprimer les notes et les évènements créés par la personne qui quitte la coloc
        eventRepository.deleteByCreator_IdAndSharedHouse_Id(
            me.getId(), house.getId() 
        ); 
        eventRepository.flush();
        noteRepository.deleteAll(
                noteRepository.findByCreator_Id(me.getId())
        );
        noteRepository.flush();

        boolean lastMember = roommateRepository.findBySharedHouseId(house.getId()).size() <= 1;
        if (!lastMember) {
            checkCanLeave(me, house.getId());
            handOverPots(me.getId(), house.getId());
        }
        taskService.unassignToDoTasksOf(house.getId(), me.getId());
        articleService.deletePersonalArticlesOf(house.getId(), me.getId());

        me.setSharedHouse(null);
        roommateRepository.save(me);

        // Le dernier qui part supprime la coloc (et tout son contenu, grâce aux ON DELETE CASCADE)
        if (roommateRepository.findBySharedHouseId(house.getId()).isEmpty()) {
            sharedHouseRepository.delete(house);
        }
    }

        /**
     * On ne peut partir que si on est quitte : solde des dépenses à 0
     * et plus d'argent dans une cagnotte dont l'achat n'est pas encore fait.
     */
    private void checkCanLeave(Roommate me, Long houseId) {
        List<String> todo = new ArrayList<>();

        BigDecimal balance = balanceService.computeBalances(houseId).getOrDefault(me.getId(), BigDecimal.ZERO);
        if (balance.signum() < 0) {
            todo.add("remboursez les " + euros(balance.negate()) + " que vous devez");
        } else if (balance.signum() > 0) {
            todo.add("faites-vous rembourser les " + euros(balance) + " qu'on vous doit");
        }

        Map<String, BigDecimal> paidByPot = new LinkedHashMap<>();
        for (PotPayment payment : potPaymentRepository
                .findByRoommateIdAndPotSharedHouseIdAndPotPurchasedFalse(me.getId(), houseId)) {
            paidByPot.merge(payment.getPot().getName(), payment.getAmount(), BigDecimal::add);
        }
        if (!paidByPot.isEmpty()) {
            List<String> pots = paidByPot.entrySet().stream()
                    .map(entry -> "« " + entry.getKey() + " » (" + euros(entry.getValue()) + ")")
                    .toList();
            todo.add("récupérez votre argent " + (pots.size() == 1 ? "de la cagnotte " : "des cagnottes ")
                    + String.join(", ", pots) + " ou attendez que l'achat soit fait");
        }

        if (!todo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Avant de quitter la colocation, " + String.join(" et ", todo) + ".");
        }
    }

    /**
     * Cagnottes au départ d'un coloc :
     * - il est retiré des cagnottes en cours (les parts sont recalculées entre ceux qui restent) ;
     * - celles qu'il a créées passent à un autre participant (sinon à un autre membre de la coloc),
     *   pour qu'il reste quelqu'un capable de les modifier ou de les supprimer.
     */
    private void handOverPots(Long roommateId, Long houseId) {
        List<Long> others = roommateRepository.findBySharedHouseId(houseId).stream()
                .map(Roommate::getId)
                .filter(id -> !id.equals(roommateId))
                .sorted()
                .toList();

        for (Pot pot : potRepository.findBySharedHouseIdOrderByIdDesc(houseId)) {
            if (!pot.isPurchased()) {
                pot.getParticipantIds().remove(roommateId);
            }
            if (pot.getCreatedById().equals(roommateId)) {
                Long newOwner = pot.getParticipantIds().stream()
                        .filter(others::contains)
                        .sorted()
                        .findFirst()
                        .orElseGet(() -> others.stream()
                                .filter(id -> !pot.getHiddenFromIds().contains(id))
                                .findFirst()
                                .orElse(others.get(0)));
                pot.setCreatedById(newOwner);
            }
        }
    }

    private static String euros(BigDecimal amount) {
        return String.format(Locale.FRANCE, "%.2f €", amount);
    }

    private void checkHasNoHouse(Roommate roommate) {
        if (roommate.getSharedHouse() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Vous faites déjà partie d'une colocation");
        }
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (sharedHouseRepository.existsByInvitationCode(code));
        return code;
    }

    private SharedHouseResponse toResponse(SharedHouse house) {
        return SharedHouseResponse.from(house, roommateRepository.findBySharedHouseId(house.getId()));
    }
}