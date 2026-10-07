package com.coloc.back.service;

import com.coloc.back.dto.AuthResponse;
import com.coloc.back.dto.LoginRequest;
import com.coloc.back.dto.RegisterRequest;
import com.coloc.back.dto.RoommateResponse;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RoommateRepository roommateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (roommateRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }

        Roommate roommate = new Roommate();
        roommate.setName(request.name().trim());
        roommate.setSurname(request.surname().trim());
        roommate.setEmail(email);
        roommate.setPassword(passwordEncoder.encode(request.password()));
        roommate.setBirthday(request.birthday().atStartOfDay());
        roommate.setPoints(0);

        Roommate saved = roommateRepository.save(roommate);

        return new AuthResponse(jwtService.generateToken(saved.getId()), RoommateResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Roommate roommate = roommateRepository.findByEmail(email)
                .filter(r -> passwordEncoder.matches(request.password(), r.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));

        return new AuthResponse(jwtService.generateToken(roommate.getId()), RoommateResponse.from(roommate));
    }
}