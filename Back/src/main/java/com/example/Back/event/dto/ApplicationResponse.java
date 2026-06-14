package com.example.Back.event.dto;

import com.example.Back.event.entity.EventApplication;

import java.time.LocalDateTime;
/**
 * Application DTO returned for owner moderation and user dashboard.
 */


public record ApplicationResponse(
        Long id,
        Long eventId,
        String username,
        String message,
        String status,
        LocalDateTime createdAt
) {
    public static ApplicationResponse from(EventApplication a, String username) {
        return new ApplicationResponse(
                a.getId(), a.getEventId(), username,
                a.getMessage(), a.getStatus(), a.getCreatedAt()
        );
    }
}
