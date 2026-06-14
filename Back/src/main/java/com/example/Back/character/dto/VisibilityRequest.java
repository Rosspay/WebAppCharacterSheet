package com.example.Back.character.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
/**
 * Request payload to change character visibility and the list of allowed usernames.
 */


public record VisibilityRequest(
        @NotNull @Pattern(regexp = "PRIVATE|PUBLIC|RESTRICTED") String visibility,
        List<String> allowedUsernames
) {}
