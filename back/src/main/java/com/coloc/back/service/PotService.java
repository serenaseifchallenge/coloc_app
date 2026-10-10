package com.coloc.back.service;

import com.coloc.back.dto.PotParticipantResponse;
import com.coloc.back.dto.PotPaymentRequest;
import com.coloc.back.dto.PotPaymentResponse;
import com.coloc.back.dto.PotRequest;
import com.coloc.back.dto.PotResponse;
import com.coloc.back.dto.RoommateSummaryResponse;
import com.coloc.back.entity.Pot;
import com.coloc.back.entity.PotPayment;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.PotPaymentRepository;
import com.coloc.back.repository.PotRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.util.ExpenseSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PotService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PotRepository potRepository;
    private final PotPaymentRepository potPaymentRepository;
    private final RoommateRepository roommateRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<PotResponse> getPots() {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Long meId = currentUserService.getCurrentRoommateId();

        List<Pot> pots = potRepository.findBySharedHouseIdOrderByIdDesc(houseId);
        if (pots.isEmpty()) {
            return List.of();
        }

        Map<Long, List<PotPayment>> paymentsByPot = potPaymentRepository
                .findByPotIdInOrderByPaymentDateDescIdDesc(pots.stream().map(Pot::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(payment -> payment.getPot().getId()));
        Map<Long, Roommate> members = membersOf(houseId);

        return pots.stream()
                .map(pot -> toResponse(pot, paymentsByPot.getOrDefault(pot.getId(), List.of()), members, meId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PotResponse getPot(Long id) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Pot pot = potRepository.findByIdAndSharedHouseId(id, houseId).orElseThrow(this::notFound);
        return toResponse(pot, paymentsOf(pot), membersOf(houseId), currentUserService.getCurrentRoommateId());
    }

    @Transactional
    public PotResponse createPot(PotRequest request) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Long meId = currentUserService.getCurrentRoommateId();
        Map<Long, Roommate> members = membersOf(houseId);
        validate(request, members);

        Pot pot = new Pot();
        pot.setSharedHouseId(houseId);
        pot.setCreatedById(meId);
        apply(pot, request);

        Pot saved = potRepository.save(pot);
        return toResponse(saved, List.of(), members, meId);
    }

    @Transactional
    public PotResponse updatePot(Long id, PotRequest request) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Long meId = currentUserService.getCurrentRoommateId();
        Pot pot = potRepository.findForUpdateByIdAndSharedHouseId(id, houseId).orElseThrow(this::notFound);
        requireCreator(pot, meId);

        Map<Long, Roommate> members = membersOf(houseId);
        validate(request, members);

        List<PotPayment> payments = paymentsOf(pot);
        BigDecimal collected = sum(payments);
        if (request.targetAmount().compareTo(collected) < 0) {
            throw badRequest("L'objectif ne peut pas être inférieur au montant déjà récolté (" + collected + " €)");
        }
        Set<Long> payers = payments.stream().map(payment -> payment.getRoommate().getId()).collect(Collectors.toSet());
        if (!request.participantIds().containsAll(payers)) {
            throw badRequest("Impossible de retirer un participant qui a déjà versé de l'argent");
        }

        apply(pot, request);
        return toResponse(pot, payments, members, meId);
    }

    @Transactional
    public void deletePot(Long id) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Pot pot = potRepository.findForUpdateByIdAndSharedHouseId(id, houseId).orElseThrow(this::notFound);
        requireCreator(pot, currentUserService.getCurrentRoommateId());

        potPaymentRepository.deleteByPotId(pot.getId());
        potRepository.delete(pot);
    }

    @Transactional
    public PotResponse addPayment(Long id, PotPaymentRequest request) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Long meId = currentUserService.getCurrentRoommateId();
        Pot pot = potRepository.findForUpdateByIdAndSharedHouseId(id, houseId).orElseThrow(this::notFound);

        if (!pot.getParticipantIds().contains(meId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne participez pas à cette cagnotte");
        }
        if (request.amount().compareTo(MAX_AMOUNT) > 0) {
            throw badRequest("Le montant est trop élevé");
        }

        BigDecimal remaining = pot.getTargetAmount().subtract(sum(paymentsOf(pot)));
        if (request.amount().compareTo(remaining) > 0) {
            throw badRequest("Il ne reste que " + remaining.max(BigDecimal.ZERO) + " € à collecter");
        }

        PotPayment payment = new PotPayment();
        payment.setPot(pot);
        payment.setRoommate(currentUserService.getCurrentRoommate());
        payment.setAmount(request.amount().setScale(2));
        payment.setMethod(request.method());
        payment.setPaymentDate(request.paymentDate());
        potPaymentRepository.save(payment);

        return toResponse(pot, paymentsOf(pot), membersOf(houseId), meId);
    }

    private List<PotPayment> paymentsOf(Pot pot) {
        return potPaymentRepository.findByPotIdInOrderByPaymentDateDescIdDesc(List.of(pot.getId()));
    }

    private Map<Long, Roommate> membersOf(Long houseId) {
        return roommateRepository.findBySharedHouseId(houseId).stream()
                .collect(Collectors.toMap(Roommate::getId, Function.identity()));
    }

    private void validate(PotRequest request, Map<Long, Roommate> members) {
        if (request.targetAmount().compareTo(MAX_AMOUNT) > 0) {
            throw badRequest("L'objectif est trop élevé");
        }
        Set<Long> unique = new HashSet<>(request.participantIds());
        if (unique.size() != request.participantIds().size()) {
            throw badRequest("Un participant est présent plusieurs fois");
        }
        if (!members.keySet().containsAll(unique)) {
            throw badRequest("Un participant ne fait pas partie de votre colocation");
        }
    }

    private void apply(Pot pot, PotRequest request) {
        pot.setName(request.name().trim());
        pot.setType(request.type());
        pot.setTargetAmount(request.targetAmount().setScale(2));
        pot.setDeadline(request.deadline());
        pot.getParticipantIds().clear();
        pot.getParticipantIds().addAll(request.participantIds());
    }

    private void requireCreator(Pot pot, Long meId) {
        if (!pot.getCreatedById().equals(meId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul le créateur de la cagnotte peut la modifier ou la supprimer");
        }
    }

    private BigDecimal sum(List<PotPayment> payments) {
        return payments.stream().map(PotPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PotResponse toResponse(Pot pot, List<PotPayment> payments, Map<Long, Roommate> members, Long meId) {
        List<Long> ids = pot.getParticipantIds().stream().sorted().toList();
        Map<Long, BigDecimal> shares = ids.isEmpty()
                ? Collections.emptyMap()
                : ExpenseSplitter.splitEqually(pot.getTargetAmount(), ids);

        Map<Long, BigDecimal> paidBy = new HashMap<>();
        for (PotPayment payment : payments) {
            paidBy.merge(payment.getRoommate().getId(), payment.getAmount(), BigDecimal::add);
        }

        BigDecimal target = pot.getTargetAmount();
        BigDecimal collected = sum(payments);
        BigDecimal remaining = target.subtract(collected).max(BigDecimal.ZERO);
        int percent = Math.min(100,
                collected.multiply(HUNDRED).divide(target, 0, RoundingMode.DOWN).intValue());
        BigDecimal sharePerPerson = shares.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal remainingPerPerson = ids.isEmpty()
                ? BigDecimal.ZERO
                : remaining.divide(BigDecimal.valueOf(ids.size()), 2, RoundingMode.CEILING);

        List<PotParticipantResponse> participants = ids.stream()
                .filter(members::containsKey)
                .map(roommateId -> new PotParticipantResponse(
                        RoommateSummaryResponse.from(members.get(roommateId)),
                        paidBy.getOrDefault(roommateId, BigDecimal.ZERO),
                        shares.get(roommateId)))
                .toList();

        List<PotPaymentResponse> paymentResponses = payments.stream()
                .map(payment -> new PotPaymentResponse(
                        payment.getId(),
                        RoommateSummaryResponse.from(payment.getRoommate()),
                        payment.getAmount(),
                        payment.getMethod(),
                        payment.getPaymentDate()))
                .toList();

        boolean participant = ids.contains(meId);
        return new PotResponse(
                pot.getId(),
                pot.getName(),
                pot.getType(),
                target,
                pot.getDeadline(),
                sharePerPerson,
                collected,
                percent,
                remaining,
                remainingPerPerson,
                participant,
                participant ? shares.get(meId) : null,
                paidBy.getOrDefault(meId, BigDecimal.ZERO),
                pot.getCreatedById().equals(meId),
                ids,
                participants,
                paymentResponses
        );
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Cagnotte introuvable");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
