package com.coloc.back.service;

import com.coloc.back.dto.RoommateResponse;
import com.coloc.back.dto.UpdateCredentialsRequest;
import com.coloc.back.dto.UpdateProfileRequest;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.RoommateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RoommateService {

    private final RoommateRepository roommateRepository;
    private final CurrentUserService currentUserService;
    private final SharedHouseService sharedHouseService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public RoommateResponse getMe() {
        return RoommateResponse.from(currentUserService.getCurrentRoommate());
    }

    @Transactional
    public RoommateResponse updateMe(UpdateProfileRequest request) {
        Roommate me = currentUserService.getCurrentRoommate();
        me.setName(request.name().trim());
        me.setSurname(request.surname().trim());
        me.setBirthday(request.birthday().atStartOfDay());
        return RoommateResponse.from(roommateRepository.save(me));
    }

    /** Pop-up « Editer profil » : changer d'email et/ou de mot de passe. */
    @Transactional
    public RoommateResponse updateCredentials(UpdateCredentialsRequest request) {
        Roommate me = currentUserService.getCurrentRoommate();
        String email = request.email().trim().toLowerCase();

        if (!email.equals(me.getEmail()) && roommateRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        me.setEmail(email);

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            me.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        return RoommateResponse.from(roommateRepository.save(me));
    }

    /** Bouton « Supprimer ce compte ». */
    @Transactional
    public void deleteMe() {
        Roommate me = currentUserService.getCurrentRoommate();
        if (me.getSharedHouse() != null) {
            sharedHouseService.leave(); // supprime aussi la coloc si c'était le dernier membre
        }
        try {
            roommateRepository.delete(me);
            roommateRepository.flush(); // force la suppression maintenant pour attraper l'erreur ici
        } catch (DataIntegrityViolationException e) {
            // Le compte est encore référencé (dépense payée, note, événement...) : on annule tout.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer ce compte : il est encore lié à des dépenses, notes ou événements de la coloc.");
        }
    }
}