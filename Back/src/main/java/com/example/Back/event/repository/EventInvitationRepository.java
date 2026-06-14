package com.example.Back.event.repository;

import com.example.Back.event.entity.EventInvitation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * Reactive repository for event invitations.
 */

public interface EventInvitationRepository
        extends ReactiveCrudRepository<EventInvitation, Long> {

    Flux<EventInvitation> findAllByEventId(Long eventId);

    Flux<EventInvitation> findAllByUserId(Long userId);

    Mono<EventInvitation> findByEventIdAndUserId(Long eventId, Long userId);

    Mono<Boolean> existsByEventIdAndUserId(Long eventId, Long userId);

    Mono<Void> deleteAllByEventId(Long eventId);
}
