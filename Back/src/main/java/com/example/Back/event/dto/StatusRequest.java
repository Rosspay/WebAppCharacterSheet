package com.example.Back.event.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
/**
 * Generic status-change request payload (e.g. for invitations or applications).
 */

public record StatusRequest(
        @NotNull @Pattern(regexp = "ACCEPTED|REJECTED|DECLINED") String status
) {}
