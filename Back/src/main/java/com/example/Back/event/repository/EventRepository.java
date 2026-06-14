package com.example.Back.event.repository;

import com.example.Back.event.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * Reactive repository for events with search and filtering.
 */

public interface EventRepository extends ReactiveCrudRepository<Event, Long> {

    Flux<Event> findAllByOwnerId(Long ownerId);

    Flux<Event> findAllByEventType(String eventType, Pageable pageable);

    Mono<Long> countByEventType(String eventType);

    @Query("SELECT * FROM events WHERE event_type = 'OPEN' AND " +
            "(LOWER(title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(COALESCE(description,'')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(COALESCE(location,'')) LIKE LOWER(CONCAT('%', :query, '%')))")
    Flux<Event> searchOpen(String query, Pageable pageable);

    Mono<Boolean> existsByIdAndOwnerId(Long id, Long ownerId);
}
