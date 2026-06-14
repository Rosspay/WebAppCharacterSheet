package com.example.Back.integration;

import com.example.Back.dto.RegisterRequest;
import com.example.Back.dto.TokenResponse;
import com.example.Back.template.dto.TemplateRequest;
import com.example.Back.template.entity.TemplateNode;
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

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
class TemplateControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nri_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private WebTestClient webClient;


    private String registerAndLogin(String username) {
        var req = new RegisterRequest(username, username + "@example.com", "password1");
        TokenResponse tokens = webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(req)
                .exchange().expectStatus().isCreated()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();
        assertThat(tokens).isNotNull();
        return "Bearer " + tokens.accessToken();
    }


    private Long createTemplate(String token, TemplateRequest req) {
        JsonNode body = webClient.post().uri("/api/v1/templates")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(req)
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(body).isNotNull();
        return body.get("id").asLong();
    }

    @Test
    @DisplayName("IT-08, IT-18: создание шаблона со всеми типами узлов + чтение JSONB")
    void createAndReadTemplate_withAllNodeTypes() {
        String token = registerAndLogin("it_tpl_owner");

        var nodes = List.<TemplateNode>of(
                new TemplateNode.ContainerNode("c1", 0, "Атрибуты", "CONTAINER",
                        List.of(
                                new TemplateNode.BlockNode("b1", 0, "Сила", "BLOCK", 10),
                                new TemplateNode.CounterNode("cnt1", 1, "ОЗ", "COUNTER", 5, 10)
                        )),
                new TemplateNode.TableNode("t1", 1, "Инвентарь", "TABLE", 3, 3),
                new TemplateNode.TextFieldNode("tf1", 2, "TEXT_FIELD", "Описание...")
        );

        Long id = createTemplate(token, new TemplateRequest("DnD 5e", "desc", nodes));


        webClient.get().uri("/api/v1/templates/" + id)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("DnD 5e")
                .jsonPath("$.content[0].type").isEqualTo("CONTAINER")
                .jsonPath("$.content[0].children[0].type").isEqualTo("BLOCK")
                .jsonPath("$.content[0].children[1].type").isEqualTo("COUNTER")
                .jsonPath("$.content[1].type").isEqualTo("TABLE")
                .jsonPath("$.content[2].type").isEqualTo("TEXT_FIELD");
    }

    @Test
    @DisplayName("IT-09: PUT чужого шаблона → 403 Forbidden")
    void updateForeignTemplate_forbidden() {
        String tokenA = registerAndLogin("it_tpl_a");
        String tokenB = registerAndLogin("it_tpl_b");

        Long id = createTemplate(tokenA, new TemplateRequest("T", "d", List.of()));

        webClient.put().uri("/api/v1/templates/" + id)
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TemplateRequest("hijacked", "d", List.of()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("IT-10: DELETE своего шаблона → 204 + последующий GET → 404")
    void deleteOwnTemplate_thenNotFound() {
        String token = registerAndLogin("it_tpl_del");
        Long id = createTemplate(token, new TemplateRequest("T", "d", List.of()));

        webClient.delete().uri("/api/v1/templates/" + id)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNoContent();

        webClient.get().uri("/api/v1/templates/" + id)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("IT-11: GET /templates/public?query=... ищет по подстроке")
    void publicTemplates_searchByQuery() {
        String token = registerAndLogin("it_tpl_search");

        Long id = createTemplate(token,
                new TemplateRequest("UNIQUEMARKER_42 5e", "d", List.of()));


        webClient.patch().uri("/api/v1/templates/" + id + "/publish")
                .header("Authorization", token)
                .exchange().expectStatus().isOk();

        webClient.get().uri("/api/v1/templates/public?query=UNIQUEMARKER_42")
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].title").isEqualTo("UNIQUEMARKER_42 5e");
    }

    @Test
    @DisplayName("IT-17: запрос с некорректным токеном → 401 + структурированное тело")
    void invalidToken_returns401() {
        webClient.get().uri("/api/v1/templates/my")
                .header("Authorization", "Bearer invalid.jwt.token")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }
}
