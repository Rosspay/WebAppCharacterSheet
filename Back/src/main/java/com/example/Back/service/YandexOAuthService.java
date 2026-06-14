package com.example.Back.service;

import com.example.Back.dto.YandexTokenResponse;
import com.example.Back.dto.YandexUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
/**
 * HTTP client for the Yandex OAuth API: exchanges authorization codes for access tokens and fetches the user profile.
 */


@Service
@Slf4j
public class YandexOAuthService {

    private final WebClient tokenClient;
    private final WebClient userInfoClient;
    private final String clientId;
    private final String clientSecret;

    public YandexOAuthService(
            WebClient.Builder builder,
            @Value("${app.oauth.yandex.client-id:}")     String clientId,
            @Value("${app.oauth.yandex.client-secret:}") String clientSecret,
            @Value("${app.oauth.yandex.token-uri:https://oauth.yandex.ru/token}")
            String tokenUri,
            @Value("${app.oauth.yandex.user-info-uri:https://login.yandex.ru/info}")
            String userInfoUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenClient = builder.clone().baseUrl(tokenUri).build();
        this.userInfoClient = builder.clone().baseUrl(userInfoUri).build();
    }


    /**
     * Exchanges a Yandex OAuth authorization code for an access token.
     *
     * <p>Posts an {@code application/x-www-form-urlencoded} body with
     * {@code grant_type=authorization_code} to {@code app.oauth.yandex.token-uri}.
     * When {@code redirectUri} is provided it is forwarded back to Yandex so
     * the value matches the one used during the authorization request — a
     * mismatch results in {@code invalid_grant} on the Yandex side.
     *
     * @param code        authorization code returned by Yandex
     * @param redirectUri redirect URI used during authorization, or {@code null}
     * @return deserialized Yandex token response (access/refresh tokens, expiration)
     * @throws IllegalStateException if Yandex OAuth is not configured (no client-id)
     */
    public Mono<YandexTokenResponse> exchangeCodeForToken(String code, String redirectUri) {
        if (clientId == null || clientId.isBlank()) {
            return Mono.error(new IllegalStateException(
                    "Yandex OAuth is not configured: app.oauth.yandex.client-id is empty"));
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "authorization_code");
        form.add("code",          code);
        form.add("client_id",     clientId);
        form.add("client_secret", clientSecret);
        if (redirectUri != null && !redirectUri.isBlank()) {
            form.add("redirect_uri", redirectUri);
        }
        return tokenClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(YandexTokenResponse.class);
    }


    /**
     * Fetches the Yandex user profile using a previously obtained access token.
     *
     * @param accessToken Yandex access token (used as {@code Authorization: OAuth <token>})
     * @return user profile (id, login, default e-mail, display name)
     */
    public Mono<YandexUserInfo> fetchUserInfo(String accessToken) {
        return userInfoClient.get()
                .uri(uri -> uri.queryParam("format", "json").build())
                .header("Authorization", "OAuth " + accessToken)
                .retrieve()
                .bodyToMono(YandexUserInfo.class);
    }
}
