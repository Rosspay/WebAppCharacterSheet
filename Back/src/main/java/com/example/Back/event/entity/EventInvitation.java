package com.example.Back.event.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
/**
 * Event invitation entity persisted in the {@code event_invitations} table.
 */


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("event_invitations")
public class EventInvitation {
    @Id
    private Long id;
    private Long eventId;
    private Long userId;
    @Builder.Default
    private String status = "INVITED";
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
