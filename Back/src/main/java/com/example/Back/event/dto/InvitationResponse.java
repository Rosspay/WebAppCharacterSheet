package com.example.Back.event.dto;

import com.example.Back.event.entity.EventInvitation;

import java.time.LocalDateTime;
/**
 * Invitation DTO returned both to the owner and to the invitee.
 */


public record InvitationResponse(
        Long id,
        Long eventId,
        String username,
        String status,
        LocalDateTime createdAt
) {
    public static InvitationResponse from(EventInvitation i, String username) {
        return new InvitationResponse(
                i.getId(), i.getEventId(), username,
                i.getStatus(), i.getCreatedAt()
        );
    }
}
