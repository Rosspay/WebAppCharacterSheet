package com.example.Back.dto;

import jakarta.validation.constraints.NotBlank;
/**
 * Yandex OAuth callback payload: authorization code together with the redirect URI used during the authorization request.
 */

public record YandexCallbackRequest(
        @NotBlank String code,
        String redirectUri
) {}
