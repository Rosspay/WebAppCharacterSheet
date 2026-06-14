package com.example.Back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Yandex OAuth token endpoint response (deserialization target).
 */

public record YandexTokenResponse(
        @JsonProperty("access_token")  String accessToken,
        @JsonProperty("expires_in")    Long   expiresIn,
        @JsonProperty("token_type")    String tokenType,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("scope")         String scope
) {}
