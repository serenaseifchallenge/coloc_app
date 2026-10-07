package com.coloc.back.service;

import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.RoommateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final RoommateRepository roommateRepository;

    public Long getCurrentRoommateId() {
        return 1L; // TEMPORAIRE : Alice, en attendant le JWT
    }

    public Roommate getCurrentRoommate() {
        return roommateRepository.findById(getCurrentRoommateId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    public SharedHouse getCurrentSharedHouse() {
        SharedHouse house = getCurrentRoommate().getSharedHouse();
        if (house == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Vous n'avez pas encore de colocation");
        }
        return house;
    }
}