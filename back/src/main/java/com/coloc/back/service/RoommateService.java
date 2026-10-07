package com.coloc.back.service;

import com.coloc.back.dto.RoommateResponse;
import com.coloc.back.dto.UpdateProfileRequest;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.RoommateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateService {

    private final RoommateRepository roommateRepository;
    private final CurrentUserService currentUserService;

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
}