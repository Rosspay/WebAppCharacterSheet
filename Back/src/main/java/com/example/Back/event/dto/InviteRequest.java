package com.example.Back.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * Request payload to invite a user by username.
 */


public record InviteRequest(
        @NotBlank @Size(max = 50) String username
) {}
