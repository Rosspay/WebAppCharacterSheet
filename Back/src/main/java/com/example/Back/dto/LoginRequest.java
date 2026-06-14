package com.example.Back.dto;

import jakarta.validation.constraints.NotBlank;
/**
 * Login request payload (username + password).
 */

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}