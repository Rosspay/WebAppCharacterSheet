package com.example.Back.character.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CharacterRequest(
        @NotNull Long templateId,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @Pattern(regexp = "PRIVATE|PUBLIC|RESTRICTED") String visibility,
        @NotNull Map<String, Object> fieldValues,
        List<Long> allowedUserIds
) {}
