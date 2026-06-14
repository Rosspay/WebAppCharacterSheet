package com.example.Back.event.dto;

import jakarta.validation.constraints.Size;
/**
 * Request payload to apply for an open event (optional message).
 */

public record ApplicationRequest(
        @Size(max = 1000) String message
) {}
