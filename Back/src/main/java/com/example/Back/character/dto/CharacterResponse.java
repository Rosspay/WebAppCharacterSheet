package com.example.Back.character.dto;

import com.example.Back.character.entity.Character;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * Full character DTO with field values and the list of allowed usernames.
 */


public record CharacterResponse(
        Long id,
        Long ownerId,
        Long templateId,
        String name,
        String description,
        String visibility,
        Map<String, Object> fieldValues,
        List<String> allowedUsernames,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CharacterResponse from(Character c, List<String> allowedUsernames) {
        return new CharacterResponse(
                c.getId(), c.getOwnerId(), c.getTemplateId(),
                c.getName(), c.getDescription(), c.getVisibility(),
                c.getFieldValues(), allowedUsernames,
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
