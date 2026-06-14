package com.example.Back.event.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
/**
 * Event entity persisted in the {@code events} table.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("events")
public class Event {
    @Id
    private Long id;

    private Long ownerId;

    private String title;

    private String description;

    private String location;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;


    @Builder.Default
    private String eventType = "CLOSED";


    @Builder.Default
    private boolean allowApplications = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
