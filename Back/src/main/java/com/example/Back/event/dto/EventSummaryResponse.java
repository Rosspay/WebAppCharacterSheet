package com.example.Back.event.dto;

import com.example.Back.event.entity.Event;

import java.time.LocalDateTime;
/**
 * Summary event DTO used for feeds and listings.
 */

public record EventSummaryResponse(
        Long id,
        Long ownerId,
        String title,
        String description,
        String location,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String eventType,
        boolean allowApplications,
        LocalDateTime createdAt
) {
    public static EventSummaryResponse from(Event e) {
        return new EventSummaryResponse(
                e.getId(), e.getOwnerId(), e.getTitle(), e.getDescription(),
                e.getLocation(), e.getStartsAt(), e.getEndsAt(),
                e.getEventType(), e.isAllowApplications(),
                e.getCreatedAt()
        );
    }
}
