package com.example.Back.dto;

import jakarta.validation.constraints.NotBlank;
/**
 * Refresh request payload containing the refresh token to rotate.
 */

public record RefreshRequest(@NotBlank String refreshToken) {}
