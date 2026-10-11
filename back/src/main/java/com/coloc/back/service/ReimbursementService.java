package com.coloc.back.service;

import com.coloc.back.dto.ReimbursementRequest;
import com.coloc.back.dto.ReimbursementResponse;
import com.coloc.back.dto.RoommateSummaryResponse;
import com.coloc.back.entity.Reimbursement;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.ReimbursementRepository;
import com.coloc.back.repository.RoommateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReimbursementService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

    private final ReimbursementRepository reimbursementRepository;
    private final RoommateRepository roommateRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ReimbursementResponse> getLatest(int limit) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        int size = Math.max(1, Math.min(limit, 50));

        return reimbursementRepository
                .findBySharedHouseIdOrderByReimbursementDateDescIdDesc(houseId, PageRequest.of(0, size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReimbursementResponse create(ReimbursementRequest request) {
        Long houseId = currentUserService.getCurrentSharedHouse().getId();
        Long meId = currentUserService.getCurrentRoommateId();
        Long payerId = request.payerId() != null ? request.payerId() : meId;

        if (payerId.equals(request.receiverId())) {
            throw badRequest("On ne peut pas se rembourser soi-même");
        }
        if (!meId.equals(payerId) && !meId.equals(request.receiverId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous pouvez seulement enregistrer un remboursement qui vous concerne");
        }
        if (request.amount().compareTo(MAX_AMOUNT) > 0) {
            throw badRequest("Le montant est trop élevé");
        }

        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setSharedHouseId(houseId);
        reimbursement.setPayer(findMember(payerId, houseId));
        reimbursement.setReceiver(findMember(request.receiverId(), houseId));
        reimbursement.setAmount(request.amount().setScale(2));
        reimbursement.setMethod(request.method());
        reimbursement.setReimbursementDate(request.reimbursementDate());

        return toResponse(reimbursementRepository.save(reimbursement));
    }

    private Roommate findMember(Long roommateId, Long houseId) {
        return roommateRepository.findById(roommateId)
                .filter(roommate -> roommate.getSharedHouse() != null
                        && houseId.equals(roommate.getSharedHouse().getId()))
                .orElseThrow(() -> badRequest("Ce colocataire ne fait pas partie de votre colocation"));
    }

    private ReimbursementResponse toResponse(Reimbursement reimbursement) {
        return new ReimbursementResponse(
                reimbursement.getId(),
                RoommateSummaryResponse.from(reimbursement.getPayer()),
                RoommateSummaryResponse.from(reimbursement.getReceiver()),
                reimbursement.getAmount(),
                reimbursement.getMethod(),
                reimbursement.getReimbursementDate()
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
