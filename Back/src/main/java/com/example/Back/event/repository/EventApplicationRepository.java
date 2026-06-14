package com.example.Back.event.repository;

import com.example.Back.event.entity.EventApplication;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * Reactive repository for event applications.
 */

public interface EventApplicationRepository
        extends ReactiveCrudRepository<EventApplication, Long> {

    Flux<EventApplication> findAllByEventId(Long eventId);

    Flux<EventApplication> findAllByUserId(Long userId);

    Mono<EventApplication> findByEventIdAndUserId(Long eventId, Long userId);

    Mono<Boolean> existsByEventIdAndUserId(Long eventId, Long userId);

    Mono<Void> deleteAllByEventId(Long eventId);
}
