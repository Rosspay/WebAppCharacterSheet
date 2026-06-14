package com.example.Back.template.dto;

import com.example.Back.template.entity.Template;
import com.example.Back.template.entity.TemplateNode;

import java.time.LocalDateTime;
import java.util.List;
/**
 * Full template DTO with the node tree.
 */

public record TemplateResponse(
        Long               id,
        Long               ownerId,
        String             title,
        String             description,
        boolean            isPublic,
        List<TemplateNode> content,
        LocalDateTime      createdAt,
        LocalDateTime      updatedAt
) {
    public static TemplateResponse from(Template t) {
        return new TemplateResponse(
                t.getId(), t.getOwnerId(), t.getTitle(),
                t.getDescription(), t.isPublic(), t.getContent(),
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}