package com.coloc.back.service;

import com.coloc.back.dto.JoinSharedHouseRequest;
import com.coloc.back.dto.SharedHouseRequest;
import com.coloc.back.dto.SharedHouseResponse;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.repository.SharedHouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

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

        taskService.unassignToDoTasksOf(house.getId(), me.getId());
        articleService.deletePersonalArticlesOf(house.getId(), me.getId());

        me.setSharedHouse(null);
        roommateRepository.save(me);

        // Le dernier qui part supprime la coloc (et tout son contenu, grâce aux ON DELETE CASCADE)
        if (roommateRepository.findBySharedHouseId(house.getId()).isEmpty()) {
            sharedHouseRepository.delete(house);
        }
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