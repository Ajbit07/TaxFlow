package com.taxflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-that-is-long-enough-for-hs256-signing-0123456789", 30, 14);
        jwtService = new JwtService(properties);
        principal = new UserPrincipal(UUID.randomUUID(), "owner@taxflow.in", "hash", true, "BUSINESS_OWNER");
    }

    @Test
    void issuedTokenCarriesSubjectAndUserId() {
        String token = jwtService.accessToken(principal);
        assertThat(jwtService.subject(token)).isEqualTo("owner@taxflow.in");
        assertThat(jwtService.userId(token)).isEqualTo(principal.id());
        assertThat(jwtService.parse(token).get("role", String.class)).isEqualTo("BUSINESS_OWNER");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.accessToken(principal);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(Exception.class);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtService other = new JwtService(new JwtProperties(
                "another-secret-key-that-is-long-enough-for-hs256-signing-987654", 30, 14));
        String foreign = other.accessToken(principal);
        assertThatThrownBy(() -> jwtService.parse(foreign)).isInstanceOf(Exception.class);
    }
}
