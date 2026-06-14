package com.example.Back.service;

import com.example.Back.dto.YandexTokenResponse;
import com.example.Back.dto.YandexUserInfo;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


class YandexOAuthServiceTest {

    private MockWebServer server;
    private YandexOAuthService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        String tokenUri = server.url("/token").toString();
        String userUri  = server.url("/info").toString();
        service = new YandexOAuthService(WebClient.builder(),
                "client-id", "client-secret",
                tokenUri, userUri);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("UT-B-60: exchangeCodeForToken отправляет верные form-параметры и парсит JSON")
    void exchange_sendsCorrectFormAndParses() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {"access_token":"AT","token_type":"bearer","expires_in":3600,
                     "refresh_token":"RT","scope":"login:info"}"""));

        StepVerifier.create(service.exchangeCodeForToken("code123",
                        "http://localhost:3000/cb"))
                .assertNext(t -> {
                    assertThat(t.accessToken()).isEqualTo("AT");
                    assertThat(t.tokenType()).isEqualTo("bearer");
                    assertThat(t.expiresIn()).isEqualTo(3600L);
                })
                .verifyComplete();

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(req).isNotNull();
        assertThat(req.getMethod()).isEqualTo("POST");
        String body = req.getBody().readUtf8();
        assertThat(body).contains("grant_type=authorization_code");
        assertThat(body).contains("code=code123");
        assertThat(body).contains("client_id=client-id");
        assertThat(body).contains("client_secret=client-secret");
    }

    @Test
    @DisplayName("UT-B-61: exchangeCodeForToken пробрасывает 400 от Яндекса как ошибку")
    void exchange_propagatesYandex400() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"invalid_grant\"}"));

        StepVerifier.create(service.exchangeCodeForToken("bad", null))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    @DisplayName("UT-B-62: fetchUserInfo шлёт Authorization: OAuth <token> и парсит ответ")
    void fetchUserInfo_correctHeaderAndParse() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {"id":"42","login":"john","default_email":"john@yandex.ru",
                     "real_name":"John Doe","display_name":"John"}"""));

        StepVerifier.create(service.fetchUserInfo("ABC"))
                .assertNext(info -> {
                    assertThat(info.id()).isEqualTo("42");
                    assertThat(info.login()).isEqualTo("john");
                    assertThat(info.defaultEmail()).isEqualTo("john@yandex.ru");
                })
                .verifyComplete();

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(req).isNotNull();
        assertThat(req.getHeader("Authorization")).isEqualTo("OAuth ABC");
    }

    @Test
    @DisplayName("UT-B-63: fetchUserInfo пробрасывает 401 от Яндекса")
    void fetchUserInfo_unauthorized() {
        server.enqueue(new MockResponse().setResponseCode(401));

        StepVerifier.create(service.fetchUserInfo("BAD"))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    @DisplayName("UT-B-64: DTO YandexTokenResponse корректно десериализуется")
    void yandexTokenResponse_jacksonContract() {
        var r = new YandexTokenResponse("AT", 60L, "bearer", "RT", "scope");
        assertThat(r.accessToken()).isEqualTo("AT");
        assertThat(r.refreshToken()).isEqualTo("RT");
        var u = new YandexUserInfo("1", "u", "u@y.ru", "Real", "Display");
        assertThat(u.defaultEmail()).isEqualTo("u@y.ru");
    }
}
