package com.example.Back.integration;

import com.example.Back.character.dto.CharacterRequest;
import com.example.Back.character.dto.VisibilityRequest;
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
class CharacterControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nri_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private WebTestClient webClient;

    private record UserSession(String header, Long userId, String username) {}

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
        return new UserSession(header, me.get("id").asLong(), username);
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

    @Test
    @DisplayName("IT-12: POST /characters c несуществующим templateId → 404")
    void create_unknownTemplate_returns404() {
        var owner = register("it_char_t404");

        webClient.post().uri("/api/v1/characters")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CharacterRequest(999_999L, "Hero", "d",
                        "PRIVATE", Map.of(), null))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("IT-13: создание персонажа с вложенным JSONB → корректное чтение обратно")
    void create_thenRead_preservesFieldValuesJsonb() {
        var owner = register("it_char_jsonb");
        Long tplId = createPublicTemplate(owner.header());

        Map<String, Object> fieldValues = Map.of(
                "strength", 17,
                "perks", List.of("brave", "fast"),
                "notes", Map.of("origin", "elf", "level", 3)
        );

        JsonNode created = webClient.post().uri("/api/v1/characters")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CharacterRequest(tplId, "Hero", "d",
                        "PRIVATE", fieldValues, null))
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(created).isNotNull();
        Long id = created.get("id").asLong();

        webClient.get().uri("/api/v1/characters/" + id)
                .header("Authorization", owner.header())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.fieldValues.strength").isEqualTo(17)
                .jsonPath("$.fieldValues.perks[0]").isEqualTo("brave")
                .jsonPath("$.fieldValues.notes.origin").isEqualTo("elf");
    }

    @Test
    @DisplayName("IT-14: смена видимости PRIVATE → RESTRICTED + два allowedUsernames")
    void setVisibility_restricted_createsAccessRows() {
        var owner = register("it_char_r_owner");
        var guest1 = register("it_char_r_g1");
        var guest2 = register("it_char_r_g2");
        Long tplId = createPublicTemplate(owner.header());


        JsonNode created = webClient.post().uri("/api/v1/characters")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CharacterRequest(tplId, "Hero", "d",
                        "PRIVATE", Map.of(), null))
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        Long id = created.get("id").asLong();


        webClient.patch().uri("/api/v1/characters/" + id + "/visibility")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new VisibilityRequest("RESTRICTED",
                        List.of(guest1.username(), guest2.username())))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.visibility").isEqualTo("RESTRICTED");


        for (var guest : List.of(guest1, guest2)) {
            webClient.get().uri("/api/v1/characters/available")
                    .header("Authorization", guest.header())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.items[?(@.id == " + id + ")]").exists();
        }
    }

    @Test
    @DisplayName("IT-15: DELETE персонажа также каскадно удаляет character_access")
    void delete_cascadesAccess() {
        var owner = register("it_char_del_owner");
        var guest = register("it_char_del_guest");
        Long tplId = createPublicTemplate(owner.header());

        JsonNode created = webClient.post().uri("/api/v1/characters")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CharacterRequest(tplId, "Hero", "d",
                        "RESTRICTED", Map.of(), List.of(guest.username())))
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        Long id = created.get("id").asLong();


        webClient.get().uri("/api/v1/characters/" + id)
                .header("Authorization", guest.header())
                .exchange().expectStatus().isOk();


        webClient.delete().uri("/api/v1/characters/" + id)
                .header("Authorization", owner.header())
                .exchange().expectStatus().isNoContent();


        webClient.get().uri("/api/v1/characters/" + id)
                .header("Authorization", guest.header())
                .exchange().expectStatus().isNotFound();
    }
}
