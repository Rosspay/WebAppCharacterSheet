package com.example.Back.integration;

import com.example.Back.dto.LoginRequest;
import com.example.Back.dto.RegisterRequest;
import com.example.Back.dto.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nri_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private WebTestClient webClient;

    @Test
    @DisplayName("IT-01: POST /auth/register создаёт пользователя и возвращает токены")
    void register_persistsUserAndReturnsTokens() {
        var req = new RegisterRequest("it_user1", "it_user1@example.com", "password1");

        TokenResponse body = webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TokenResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("IT-02: повторная регистрация с тем же username → 409 Conflict")
    void register_duplicateUsername_returns409() {
        var req = new RegisterRequest("it_user2", "it_user2@example.com", "password1");
        webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(req)
                .exchange().expectStatus().isCreated();

        var dupe = new RegisterRequest("it_user2", "other@example.com", "password1");
        webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(dupe)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").isEqualTo("Username already taken");
    }

    @Test
    @DisplayName("IT-03 / IT-04: login happy-path и неверный пароль")
    void login_happyPathAndWrongPassword() {
        var register = new RegisterRequest("it_user3", "it_user3@example.com", "password1");
        webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(register)
                .exchange().expectStatus().isCreated();


        webClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("it_user3", "password1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty();


        webClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("it_user3", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("IT-06 / IT-07: /auth/me — с токеном 200, без токена 401")
    void me_requiresBearerToken() {

        webClient.get().uri("/api/v1/auth/me").exchange().expectStatus().isUnauthorized();


        var register = new RegisterRequest("it_user4", "it_user4@example.com", "password1");
        TokenResponse tokens = webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(register)
                .exchange().expectStatus().isCreated()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();
        assertThat(tokens).isNotNull();

        webClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + tokens.accessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("it_user4")
                .jsonPath("$.email").isEqualTo("it_user4@example.com")
                .jsonPath("$.role").isEqualTo("ROLE_USER");
    }
}
