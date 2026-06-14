package com.example.Back.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
/**
 * User account entity persisted in the {@code users} table.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @Id
    private Long id;
    private String username;
    private String email;

    private String password;
    @Builder.Default
    private String role = "ROLE_USER";
    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private String provider = "LOCAL";

    private String yandexId;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
