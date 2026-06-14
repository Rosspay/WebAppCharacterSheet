package com.example.Back.integration;

import com.example.Back.dto.RegisterRequest;
import com.example.Back.dto.TokenResponse;
import com.example.Back.event.dto.ApplicationRequest;
import com.example.Back.event.dto.EventRequest;
import com.example.Back.event.dto.InviteRequest;
import com.example.Back.event.dto.StatusRequest;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
class EventControllerIT {

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

    private Long createEvent(String header, String type, boolean allowApps) {
        var req = new EventRequest(
                "Test Event " + System.nanoTime(),
                "desc", "loc",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                type, allowApps);
        JsonNode created = webClient.post().uri("/api/v1/events")
                .header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(req)
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(created).isNotNull();
        return created.get("id").asLong();
    }

    @Test
    @DisplayName("IT-E-01: POST /events создаёт мероприятие и возвращает 201")
    void createEvent_returnsCreated() {
        var owner = register("evt_owner1");
        Long id = createEvent(owner.header(), "CLOSED", false);
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("IT-E-02: GET /events/{id} — владелец видит CLOSED мероприятие")
    void ownerCanGetClosedEvent() {
        var owner = register("evt_owner2");
        Long id = createEvent(owner.header(), "CLOSED", false);

        webClient.get().uri("/api/v1/events/" + id)
                .header("Authorization", owner.header())
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(id);
    }

    @Test
    @DisplayName("IT-E-03: GET /events/{id} — посторонний на CLOSED получает 403")
    void strangerForbiddenOnClosedEvent() {
        var owner    = register("evt_owner3");
        var stranger = register("evt_stranger3");
        Long id = createEvent(owner.header(), "CLOSED", false);

        webClient.get().uri("/api/v1/events/" + id)
                .header("Authorization", stranger.header())
                .exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("IT-E-04: GET /events/{id} — любой авторизованный видит OPEN")
    void anyUserCanSeeOpenEvent() {
        var owner    = register("evt_owner4");
        var visitor  = register("evt_visitor4");
        Long id = createEvent(owner.header(), "OPEN", false);

        webClient.get().uri("/api/v1/events/" + id)
                .header("Authorization", visitor.header())
                .exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("IT-E-05: PUT /events/{id} разрешено только владельцу")
    void updateOnlyByOwner() {
        var owner    = register("evt_owner5");
        var stranger = register("evt_stranger5");
        Long id = createEvent(owner.header(), "CLOSED", false);

        var update = new EventRequest(
                "Renamed", "d", "l",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "CLOSED", false);


        webClient.put().uri("/api/v1/events/" + id)
                .header("Authorization", stranger.header())
                .contentType(MediaType.APPLICATION_JSON).bodyValue(update)
                .exchange().expectStatus().isForbidden();


        webClient.put().uri("/api/v1/events/" + id)
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON).bodyValue(update)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.title").isEqualTo("Renamed");
    }

    @Test
    @DisplayName("IT-E-06: DELETE /events/{id} удаляет мероприятие и приглашения")
    void deleteCascadesInvitations() {
        var owner = register("evt_owner6");
        var guest = register("evt_guest6");
        Long id = createEvent(owner.header(), "CLOSED", false);

        webClient.post().uri("/api/v1/events/" + id + "/invitations")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new InviteRequest(guest.username()))
                .exchange().expectStatus().isCreated();

        webClient.delete().uri("/api/v1/events/" + id)
                .header("Authorization", owner.header())
                .exchange().expectStatus().isNoContent();

        webClient.get().uri("/api/v1/events/" + id)
                .header("Authorization", owner.header())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("IT-E-07: POST /events/{id}/invitations создаёт приглашение")
    void inviteCreatesInvitation() {
        var owner = register("evt_owner7");
        var guest = register("evt_guest7");
        Long id = createEvent(owner.header(), "CLOSED", false);

        webClient.post().uri("/api/v1/events/" + id + "/invitations")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new InviteRequest(guest.username()))
                .exchange().expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INVITED")
                .jsonPath("$.username").isEqualTo(guest.username());


        webClient.get().uri("/api/v1/events/" + id)
                .header("Authorization", guest.header())
                .exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("IT-E-08: PATCH /invitations/{id} — гость принимает приглашение")
    void respondToInvitationAccepts() {
        var owner = register("evt_owner8");
        var guest = register("evt_guest8");
        Long id = createEvent(owner.header(), "CLOSED", false);

        JsonNode inv = webClient.post().uri("/api/v1/events/" + id + "/invitations")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new InviteRequest(guest.username()))
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(inv).isNotNull();
        Long invId = inv.get("id").asLong();

        webClient.patch().uri("/api/v1/events/invitations/" + invId)
                .header("Authorization", guest.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new StatusRequest("ACCEPTED"))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("IT-E-09: попытка пригласить самого себя → 400")
    void selfInviteRejected() {
        var owner = register("evt_owner9");
        Long id = createEvent(owner.header(), "CLOSED", false);

        webClient.post().uri("/api/v1/events/" + id + "/invitations")
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new InviteRequest(owner.username()))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("IT-E-10: POST /events/{id}/applications работает для OPEN с allowApplications")
    void applyToOpenEvent() {
        var owner   = register("evt_owner10");
        var visitor = register("evt_visitor10");
        Long id = createEvent(owner.header(), "OPEN", true);

        webClient.post().uri("/api/v1/events/" + id + "/applications")
                .header("Authorization", visitor.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ApplicationRequest("Прошу принять"))
                .exchange().expectStatus().isCreated()
                .expectBody().jsonPath("$.status").isEqualTo("PENDING");


        webClient.get().uri("/api/v1/events/" + id + "/applications")
                .header("Authorization", owner.header())
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[0].username").isEqualTo(visitor.username());
    }

    @Test
    @DisplayName("IT-E-11: заявка на CLOSED мероприятие → 400")
    void applyToClosedRejected() {
        var owner   = register("evt_owner11");
        var visitor = register("evt_visitor11");
        Long id = createEvent(owner.header(), "CLOSED", false);

        webClient.post().uri("/api/v1/events/" + id + "/applications")
                .header("Authorization", visitor.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ApplicationRequest("Здравствуйте"))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("IT-E-12: PATCH /applications/{id} — владелец принимает заявку")
    void ownerReviewApplication() {
        var owner   = register("evt_owner12");
        var visitor = register("evt_visitor12");
        Long id = createEvent(owner.header(), "OPEN", true);

        JsonNode app = webClient.post().uri("/api/v1/events/" + id + "/applications")
                .header("Authorization", visitor.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ApplicationRequest("я хочу"))
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(app).isNotNull();
        Long appId = app.get("id").asLong();

        webClient.patch().uri("/api/v1/events/" + id + "/applications/" + appId)
                .header("Authorization", owner.header())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new StatusRequest("ACCEPTED"))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("ACCEPTED");


        webClient.get().uri("/api/v1/events/participating")
                .header("Authorization", visitor.header())
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[0].id").isEqualTo(id);
    }
}
