package com.example.Back.dto;
/**
 * Authentication response carrying the new access/refresh token pair and the access-token TTL.
 */

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
