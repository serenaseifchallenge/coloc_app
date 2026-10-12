package com.coloc.back.auth;

import com.coloc.back.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Clés de test en base64 (au moins 32 octets, comme exigé pour HS256)
    private static final String SECRET = "Y2xlLWRlLXRlc3QtcG91ci1sZXMtdGVzdHMtand0LTMybw==";
    private static final String OTHER_SECRET = "dW5lLWF1dHJlLWNsZS1zZWNyZXRlLWRlLXRlc3QtMzJvYw==";
    private static final long ONE_HOUR = 3_600_000L;

    private final JwtService jwtService = new JwtService(SECRET, ONE_HOUR);

    @Test
    @DisplayName("le token généré contient l'id du colocataire")
    void generatedTokenContainsRoommateId() {
        String token = jwtService.generateToken(42L);

        assertThat(jwtService.extractRoommateId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("token signé avec une autre clé : refusé")
    void rejectsTokenSignedWithAnotherKey() {
        String foreignToken = new JwtService(OTHER_SECRET, ONE_HOUR).generateToken(42L);

        assertThatThrownBy(() -> jwtService.extractRoommateId(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("token modifié (on remplace l'id par un autre) : refusé")
    void rejectsTamperedToken() {
        String[] parts = jwtService.generateToken(42L).split("\\.");
        String fakePayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\"}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + fakePayload + "." + parts[2];

        assertThatThrownBy(() -> jwtService.extractRoommateId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("token expiré : refusé")
    void rejectsExpiredToken() {
        String expired = new JwtService(SECRET, -1_000L).generateToken(42L);

        assertThatThrownBy(() -> jwtService.extractRoommateId(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
