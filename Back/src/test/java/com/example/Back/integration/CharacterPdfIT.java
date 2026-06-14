package com.example.Back.integration;

import com.example.Back.character.dto.CharacterRequest;
import com.example.Back.dto.RegisterRequest;
import com.example.Back.dto.TokenResponse;
import com.example.Back.template.dto.TemplateRequest;
import tools.jackson.databind.JsonNode;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
class CharacterPdfIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nri_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private WebTestClient webClient;

    private record UserSession(String header, Long userId) {}

    private UserSession register(String username) {
        var req = new RegisterRequest(username, username + "@example.com", "password1");
        TokenResponse tokens = webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(req)
                .exchange().expectStatus().isCreated()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();
        assertThat(tokens).isNotNull();
        String header = "Bearer " + tokens.accessToken();

        JsonNode me = webClient.get().uri("/api/v1/auth/me")
                .header("Authorization", header)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(me).isNotNull();
        return new UserSession(header, me.get("id").asLong());
    }

    private Long createPublicTemplate(String header) {
        JsonNode created = webClient.post().uri("/api/v1/templates")
                .header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TemplateRequest("Tmpl", "d", List.of()))
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(created).isNotNull();
        Long id = created.get("id").asLong();

        webClient.patch().uri("/api/v1/templates/" + id + "/publish")
                .header("Authorization", header)
                .exchange().expectStatus().isOk();
        return id;
    }

    private Long createCharacter(String header, Long templateId, String visibility) {
        JsonNode created = webClient.post().uri("/api/v1/characters")
                .header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CharacterRequest(templateId,
                        "Гэндальф", "Серый волшебник",
                        visibility, Map.of("note", "тест"), null))
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(created).isNotNull();
        return created.get("id").asLong();
    }

    @Test
    @DisplayName("IT-P-01: владелец скачивает PDF (200 + Content-Type: application/pdf)")
    void owner_downloadsPdf_returns200WithPdf() {
        var owner = register("pdf_owner1");
        Long tplId = createPublicTemplate(owner.header());
        Long charId = createCharacter(owner.header(), tplId, "PRIVATE");

        byte[] bytes = webClient.get().uri("/api/v1/characters/" + charId + "/pdf")
                .header("Authorization", owner.header())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .expectBody(byte[].class).returnResult().getResponseBody();

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(200);

        String head = new String(bytes, 0, 4);
        assertThat(head).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("IT-P-02: чужой пользователь на PRIVATE → 403 Forbidden")
    void stranger_onPrivate_returns403() {
        var owner    = register("pdf_owner2");
        var stranger = register("pdf_stranger2");
        Long tplId = createPublicTemplate(owner.header());
        Long charId = createCharacter(owner.header(), tplId, "PRIVATE");

        webClient.get().uri("/api/v1/characters/" + charId + "/pdf")
                .header("Authorization", stranger.header())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("IT-P-03: запрос несуществующего персонажа → 404")
    void unknownCharacter_returns404() {
        var owner = register("pdf_owner3");

        webClient.get().uri("/api/v1/characters/999999/pdf")
                .header("Authorization", owner.header())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("IT-P-04: без Bearer-токена → 401 Unauthorized")
    void noToken_returns401() {
        webClient.get().uri("/api/v1/characters/1/pdf")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
