package com.example.Back.template.dto;

import com.example.Back.template.entity.Template;

import java.time.LocalDateTime;

public record TemplateSummaryResponse(
        Long          id,
        Long          ownerId,
        String        title,
        String        description,
        boolean       isPublic,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TemplateSummaryResponse from(Template t) {
        return new TemplateSummaryResponse(
                t.getId(), t.getOwnerId(), t.getTitle(),
                t.getDescription(), t.isPublic(), t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
