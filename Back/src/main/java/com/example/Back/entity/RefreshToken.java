package com.example.Back.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
/**
 * Refresh token entity persisted in the {@code refresh_tokens} table. Each token has a signed value, an owner and an expiration timestamp.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("refresh_tokens")
public class RefreshToken {
    @Id
    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiresAt;
    @Builder.Default
    private boolean revoked = false;
}