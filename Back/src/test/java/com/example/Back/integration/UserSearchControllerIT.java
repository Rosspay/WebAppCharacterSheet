package com.example.Back.integration;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
class UserSearchControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nri_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private WebTestClient webClient;

    private String registerAndAuth(String username) {
        var req = new RegisterRequest(username, username + "@example.com", "password1");
        TokenResponse tokens = webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(req)
                .exchange().expectStatus().isCreated()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();
        assertThat(tokens).isNotNull();
        return "Bearer " + tokens.accessToken();
    }

    @Test
    @DisplayName("IT-US-01: GET /users/search возвращает совпадения по префиксу без текущего пользователя")
    void search_returnsMatchesByPrefix_excludingSelf() {
        String me = registerAndAuth("search_me");
        registerAndAuth("search_alice");
        registerAndAuth("search_bob");
        registerAndAuth("other_carol");

        String[] result = webClient.get()
                .uri(uri -> uri.path("/api/v1/users/search").queryParam("q", "search_").build())
                .header("Authorization", me)
                .exchange().expectStatus().isOk()
                .expectBody(String[].class).returnResult().getResponseBody();

        assertThat(result).isNotNull();
        assertThat(List.of(result))
                .containsExactlyInAnyOrder("search_alice", "search_bob")
                .doesNotContain("search_me", "other_carol");
    }

    @Test
    @DisplayName("IT-US-02: GET /users/search с пустым q возвращает пустой список")
    void search_emptyQuery_returnsEmpty() {
        String me = registerAndAuth("search_empty");

        String[] result = webClient.get()
                .uri(uri -> uri.path("/api/v1/users/search").queryParam("q", "").build())
                .header("Authorization", me)
                .exchange().expectStatus().isOk()
                .expectBody(String[].class).returnResult().getResponseBody();

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("IT-US-03: GET /users/search без токена → 401")
    void search_unauthenticated_returns401() {
        webClient.get()
                .uri(uri -> uri.path("/api/v1/users/search").queryParam("q", "a").build())
                .exchange().expectStatus().isUnauthorized();
    }
}
