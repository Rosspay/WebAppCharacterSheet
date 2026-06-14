package com.example.Back.event.dto;

import com.example.Back.event.entity.Event;

import java.time.LocalDateTime;
/**
 * Full event DTO including the description and counters.
 */

public record EventResponse(
        Long id,
        Long ownerId,
        String title,
        String description,
        String location,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String eventType,
        boolean allowApplications,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(), e.getOwnerId(), e.getTitle(), e.getDescription(),
                e.getLocation(), e.getStartsAt(), e.getEndsAt(),
                e.getEventType(), e.isAllowApplications(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
