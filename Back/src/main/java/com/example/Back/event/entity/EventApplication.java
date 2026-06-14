package com.example.Back.event.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
/**
 * User application to participate in an open event, persisted in the {@code event_applications} table.
 */


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("event_applications")
public class EventApplication {
    @Id
    private Long id;
    private Long eventId;
    private Long userId;
    private String message;
    @Builder.Default
    private String status = "PENDING";
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
