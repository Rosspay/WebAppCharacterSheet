package com.example.Back.template.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
/**
 * Character-sheet template entity persisted in the {@code templates} table.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("templates")
public class Template {

    @Id
    private Long id;

    private Long ownerId;

    private String title;

    private String description;

    @Builder.Default
    private boolean isPublic = false;

    @Column("content")
    @Builder.Default
    private List<TemplateNode> content = List.of();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
