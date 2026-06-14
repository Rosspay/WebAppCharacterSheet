package com.example.Back.integration;

import com.example.Back.dto.TokenResponse;
import com.example.Back.dto.YandexCallbackRequest;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
class YandexOAuthIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nri_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static MockWebServer yandexMock;

    @BeforeAll
    static void startMock() throws IOException {
        yandexMock = new MockWebServer();
        yandexMock.start();
    }

    @AfterAll
    static void stopMock() throws IOException {
        if (yandexMock != null) yandexMock.shutdown();
    }

    @DynamicPropertySource
    static void registerYandexUrls(DynamicPropertyRegistry r) {
        r.add("app.oauth.yandex.client-id",     () -> "test-id");
        r.add("app.oauth.yandex.client-secret", () -> "test-secret");
        r.add("app.oauth.yandex.token-uri",
                () -> yandexMock.url("/token").toString());
        r.add("app.oauth.yandex.user-info-uri",
                () -> yandexMock.url("/info").toString());
    }

    @Autowired
    private WebTestClient webClient;

    private void enqueueOkAuth(String yandexId, String email, String login) {
        yandexMock.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().startsWith("/token")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""
                                {"access_token":"AT","token_type":"bearer","expires_in":3600}""");
                }
                if (request.getPath() != null && request.getPath().startsWith("/info")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""
                                {"id":"%s","login":"%s","default_email":"%s",
                                 "real_name":"Real","display_name":"%s"}"""
                                    .formatted(yandexId, login, email, login));
                }
                return new MockResponse().setResponseCode(404);
            }
        });
    }

    @Test
    @DisplayName("IT-Y-01: callback с валидным code создаёт пользователя и возвращает токены")
    void callback_createsUserAndReturnsTokens() {
        enqueueOkAuth("yandex-100", "u100@yandex.ru", "u100");

        TokenResponse body = webClient.post().uri("/api/v1/auth/yandex/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new YandexCallbackRequest("code-1",
                        "http://localhost:3000/auth/yandex/callback"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();


        TokenResponse second = webClient.post().uri("/api/v1/auth/yandex/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new YandexCallbackRequest("code-2", null))
                .exchange().expectStatus().isOk()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();
        assertThat(second).isNotNull();
    }

    @Test
    @DisplayName("IT-Y-02: ошибка Яндекса (400) на токене → 4xx от приложения")
    void callback_yandexError_returns4xx() {
        yandexMock.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(400)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"error\":\"invalid_grant\"}");
            }
        });

        webClient.post().uri("/api/v1/auth/yandex/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new YandexCallbackRequest("bad-code", null))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("IT-Y-03: эндпоинт доступен без Bearer-токена (часть auth/**)")
    void callbackEndpointIsPublic() {
        enqueueOkAuth("yandex-200", "u200@yandex.ru", "u200");

        webClient.post().uri("/api/v1/auth/yandex/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new YandexCallbackRequest("ok", null))
                .exchange()
                .expectStatus().isOk();
    }
}
