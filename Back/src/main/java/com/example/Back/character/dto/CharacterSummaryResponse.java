package com.example.Back.character.dto;

import com.example.Back.character.entity.Character;

import java.time.LocalDateTime;

public record CharacterSummaryResponse(
        Long id,
        Long ownerId,
        Long templateId,
        String name,
        String description,
        String visibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CharacterSummaryResponse from(Character c) {
        return new CharacterSummaryResponse(
                c.getId(), c.getOwnerId(), c.getTemplateId(),
                c.getName(), c.getDescription(), c.getVisibility(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
