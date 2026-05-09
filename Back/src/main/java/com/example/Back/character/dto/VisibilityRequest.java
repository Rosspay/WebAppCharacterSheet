package com.example.Back.character.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record VisibilityRequest(
        @NotNull @Pattern(regexp = "PRIVATE|PUBLIC|RESTRICTED") String visibility,
        List<Long> allowedUserIds  // обязателен при visibility = RESTRICTED
) {}
