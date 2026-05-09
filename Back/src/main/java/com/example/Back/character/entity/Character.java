package com.example.Back.character.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("characters")
public class Character {
    @Id
    private Long id;

    private Long ownerId;
    private Long templateId;
    private String name;
    private String description;

    @Builder.Default
    private String visibility = "PRIVATE";

    @Column("field_values")
    @Builder.Default
    private Map<String, Object> fieldValues = Map.of();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}