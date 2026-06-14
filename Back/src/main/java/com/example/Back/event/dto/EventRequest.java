package com.example.Back.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
/**
 * Request payload to create or update an event.
 */

public record EventRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @Size(max = 255) String location,
        @NotNull LocalDateTime startsAt,
        LocalDateTime endsAt,
        @Pattern(regexp = "OPEN|CLOSED") String eventType,
        Boolean allowApplications
) {}
