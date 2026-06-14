package com.example.Back.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class JwtServiceTest {

    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRATION = 900_000L;
    private static final long REFRESH_EXPIRATION = 604_800_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("UT-B-01: generateAccessToken → extractUsername возвращает исходный username")
    void generateAccessToken_extractUsernameReturnsSubject() {
        String token = jwtService.generateAccessToken("user1", "ROLE_USER");

        assertThat(token).isNotBlank().contains(".");
        assertThat(jwtService.extractUsername(token)).isEqualTo("user1");
    }

    @Test
    @DisplayName("UT-B-02: isTokenValid возвращает true для свежего корректного токена")
    void isTokenValid_returnsTrueForFreshToken() {
        String token = jwtService.generateAccessToken("user1", "ROLE_USER");

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("UT-B-03: isTokenValid возвращает false для синтаксически некорректной строки")
    void isTokenValid_returnsFalseForMalformedToken() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
        assertThat(jwtService.isTokenValid("garbage")).isFalse();
    }

    @Test
    @DisplayName("UT-B-04: isTokenValid возвращает false для токена, подписанного другим ключом")
    void isTokenValid_returnsFalseForTokenWithDifferentSignature() {
        String foreignSecret =
                "11112222333344445555666677778888AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        JwtService foreignIssuer = new JwtService(foreignSecret,
                ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        String foreignToken = foreignIssuer.generateAccessToken("user1", "ROLE_USER");

        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }
}
