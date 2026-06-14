package com.example.Back.event.service;

import com.example.Back.event.dto.*;
import com.example.Back.event.entity.Event;
import com.example.Back.event.entity.EventApplication;
import com.example.Back.event.entity.EventInvitation;
import com.example.Back.event.repository.EventApplicationRepository;
import com.example.Back.event.repository.EventInvitationRepository;
import com.example.Back.event.repository.EventRepository;
import com.example.Back.repository.UserRepository;
import com.example.Back.template.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
/**
 * Service managing events, invitations and applications.
 */


@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventInvitationRepository invitationRepository;
    private final EventApplicationRepository applicationRepository;
    private final UserRepository userRepository;



    public Flux<EventSummaryResponse> getMyEvents(String username) {
        return resolveUserId(username)
                .flatMapMany(eventRepository::findAllByOwnerId)
                .map(EventSummaryResponse::from);
    }



    private Mono<String> usernameOf(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getUsername())
                .defaultIfEmpty("");
    }


    /**
     * Returns events the caller participates in, regardless of who initiated
     * the relationship.
     *
     * <p>Two streams are merged and deduplicated by event id:
     * <ul>
     *   <li>events where the user has an invitation in {@code INVITED} or
     *       {@code ACCEPTED} state (owner-initiated);</li>
     *   <li>events where the user has an application in {@code ACCEPTED}
     *       state (user-initiated for open events).</li>
     * </ul>
     *
     * @param username caller's username
     * @return summary DTOs ready for the "My events → Participating" tab
     */
    public Flux<EventSummaryResponse> getParticipatingEvents(String username) {
        return resolveUserId(username).flatMapMany(userId -> {
            Flux<Long> invitedEventIds = invitationRepository.findAllByUserId(userId)
                    .filter(i -> "ACCEPTED".equals(i.getStatus())
                            || "INVITED".equals(i.getStatus()))
                    .map(EventInvitation::getEventId);
            Flux<Long> acceptedAppEventIds = applicationRepository.findAllByUserId(userId)
                    .filter(a -> "ACCEPTED".equals(a.getStatus()))
                    .map(EventApplication::getEventId);

            return Flux.concat(invitedEventIds, acceptedAppEventIds)
                    .distinct()
                    .flatMap(eventRepository::findById)
                    .map(EventSummaryResponse::from);
        });
    }

    public Mono<PageResponse<EventSummaryResponse>> getOpenEvents(
            String query, int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "startsAt"));

        Flux<Event> items = (query != null && !query.isBlank())
                ? eventRepository.searchOpen(query, pageable)
                : eventRepository.findAllByEventType("OPEN", pageable);

        return Mono.zip(
                items.map(EventSummaryResponse::from).collectList(),
                eventRepository.countByEventType("OPEN")
        ).map(t -> PageResponse.of(t.getT1(), t.getT2(), page, size));
    }

    public Mono<EventResponse> getById(Long id, String username) {
        return resolveUserId(username).flatMap(userId ->
                eventRepository.findById(id)
                        .switchIfEmpty(notFound("Мероприятие не найдено"))
                        .flatMap(e -> checkAccess(e, userId)
                                .flatMap(allowed -> allowed
                                        ? Mono.just(EventResponse.from(e))
                                        : Mono.error(forbidden())))
        );
    }

    public Mono<EventResponse> create(EventRequest req, String username) {
        return validateDates(req).then(
                resolveUserId(username).flatMap(userId -> {
                    Event e = Event.builder()
                            .ownerId(userId)
                            .title(req.title())
                            .description(req.description())
                            .location(req.location())
                            .startsAt(req.startsAt())
                            .endsAt(req.endsAt())
                            .eventType(req.eventType() != null ? req.eventType() : "CLOSED")
                            .allowApplications(Boolean.TRUE.equals(req.allowApplications()))
                            .build();
                    return eventRepository.save(e).map(EventResponse::from);
                }));
    }

    public Mono<EventResponse> update(Long id, EventRequest req, String username) {
        return validateDates(req).then(
                resolveUserId(username).flatMap(userId ->
                        requireOwner(id, userId).flatMap(e -> {
                            e.setTitle(req.title());
                            e.setDescription(req.description());
                            e.setLocation(req.location());
                            e.setStartsAt(req.startsAt());
                            e.setEndsAt(req.endsAt());
                            if (req.eventType() != null) e.setEventType(req.eventType());
                            if (req.allowApplications() != null)
                                e.setAllowApplications(req.allowApplications());
                            e.setUpdatedAt(LocalDateTime.now());
                            return eventRepository.save(e).map(EventResponse::from);
                        })));
    }

    public Mono<Void> delete(Long id, String username) {
        return resolveUserId(username)
                .flatMap(userId -> requireOwner(id, userId))
                .flatMap(e -> invitationRepository.deleteAllByEventId(e.getId())
                        .then(applicationRepository.deleteAllByEventId(e.getId()))
                        .then(eventRepository.deleteById(e.getId()))
                );
    }



    /**
     * Invites another user (by username) to an event the caller owns.
     *
     * <p>The invitee is resolved to {@code users.id} so the invitation
     * survives a future username change. The operation rejects self-invites
     * and duplicates (one invitation per (event, user) pair).
     *
     * @param eventId  target event (must be owned by the caller)
     * @param req      invitation request carrying the invitee username
     * @param username caller's username
     * @return persisted invitation DTO with {@code status = INVITED}
     * @throws ResponseStatusException 404 — event not found / invitee not found,
     *                                 403 — caller is not the event owner,
     *                                 400 — self-invite or duplicate invitation
     */
    public Mono<InvitationResponse> invite(Long eventId, InviteRequest req, String username) {
        return resolveUserId(username).flatMap(ownerId ->
                requireOwner(eventId, ownerId).flatMap(e ->
                        userRepository.findByUsername(req.username())
                                .switchIfEmpty(notFound("Пользователь для приглашения не найден"))
                                .flatMap(invitee -> {
                                    if (invitee.getId().equals(ownerId)) {
                                        return Mono.error(badRequest(
                                                "Нельзя приглашать самого себя"));
                                    }
                                    return invitationRepository
                                            .existsByEventIdAndUserId(eventId, invitee.getId())
                                            .flatMap(exists -> {
                                                if (exists) {
                                                    return Mono.error(badRequest(
                                                            "Приглашение уже отправлено"));
                                                }
                                                return invitationRepository.save(
                                                        EventInvitation.builder()
                                                                .eventId(eventId)
                                                                .userId(invitee.getId())
                                                                .status("INVITED")
                                                                .build())
                                                        .map(saved -> InvitationResponse.from(
                                                                saved, invitee.getUsername()));
                                            });
                                })
                        )
                );
    }

    public Flux<InvitationResponse> getInvitations(Long eventId, String username) {
        return resolveUserId(username)
                .flatMap(userId -> requireOwner(eventId, userId))
                .flatMapMany(e -> invitationRepository.findAllByEventId(eventId))
                .flatMap(inv -> usernameOf(inv.getUserId())
                        .map(uname -> InvitationResponse.from(inv, uname)));
    }

    public Flux<InvitationResponse> getMyInvitations(String username) {
        return resolveUserId(username)
                .flatMapMany(invitationRepository::findAllByUserId)
                .map(inv -> InvitationResponse.from(inv, username));
    }

    /**
     * Records the invitee's response (ACCEPTED or DECLINED) on a pending
     * invitation.
     *
     * <p>Only the invitee themselves may respond; any other user receives
     * 403. The status payload is validated server-side: only ACCEPTED and
     * DECLINED are accepted.
     *
     * @param invitationId invitation row id
     * @param req          status payload
     * @param username     caller's username (must match the invitation user)
     * @return updated invitation DTO
     */
    public Mono<InvitationResponse> respondToInvitation(
            Long invitationId, StatusRequest req, String username) {
        if (!"ACCEPTED".equals(req.status()) && !"DECLINED".equals(req.status())) {
            return Mono.error(badRequest("Статус приглашения должен быть ACCEPTED или DECLINED"));
        }
        return resolveUserId(username).flatMap(userId ->
                invitationRepository.findById(invitationId)
                        .switchIfEmpty(notFound("Приглашение не найдено"))
                        .flatMap(inv -> {
                            if (!inv.getUserId().equals(userId)) {
                                return Mono.error(forbidden());
                            }
                            inv.setStatus(req.status());
                            return invitationRepository.save(inv);
                        })
                        .map(inv -> InvitationResponse.from(inv, username)));
    }

    public Mono<Void> cancelInvitation(Long eventId, Long invitationId, String username) {
        return resolveUserId(username).flatMap(userId ->
                requireOwner(eventId, userId).flatMap(e ->
                        invitationRepository.findById(invitationId)
                                .switchIfEmpty(notFound("Приглашение не найдено"))
                                .flatMap(inv -> {
                                    if (!inv.getEventId().equals(eventId)) {
                                        return Mono.error(badRequest(
                                                "Приглашение не относится к этому мероприятию"));
                                    }
                                    return invitationRepository.deleteById(invitationId);
                                })));
    }



    /**
     * Submits an application to participate in an OPEN event.
     *
     * <p>The event must be of type OPEN, must explicitly allow applications
     * and must not be owned by the caller. Only one application per (event,
     * user) pair is allowed; subsequent attempts return 400. The application
     * is created in PENDING state and later transitioned by the owner via
     * {@link #reviewApplication}.
     *
     * @param eventId  target event
     * @param req      optional application payload (free-form message)
     * @param username caller's username
     * @return persisted application DTO
     */
    public Mono<ApplicationResponse> apply(
            Long eventId, ApplicationRequest req, String username) {
        return resolveUserId(username).flatMap(userId ->
                eventRepository.findById(eventId)
                        .switchIfEmpty(notFound("Мероприятие не найдено"))
                        .flatMap(e -> {
                            if (!"OPEN".equals(e.getEventType())) {
                                return Mono.error(badRequest(
                                        "Заявки принимаются только в открытых мероприятиях"));
                            }
                            if (!e.isAllowApplications()) {
                                return Mono.error(badRequest(
                                        "Подача заявок отключена для этого мероприятия"));
                            }
                            if (e.getOwnerId().equals(userId)) {
                                return Mono.error(badRequest(
                                        "Владелец не может подавать заявку"));
                            }
                            return applicationRepository
                                    .existsByEventIdAndUserId(eventId, userId)
                                    .flatMap(exists -> {
                                        if (exists) {
                                            return Mono.error(badRequest(
                                                    "Заявка уже подана"));
                                        }
                                        return applicationRepository.save(
                                                EventApplication.builder()
                                                        .eventId(eventId)
                                                        .userId(userId)
                                                        .message(req != null
                                                                ? req.message() : null)
                                                        .status("PENDING")
                                                        .build())
                                                .map(saved -> ApplicationResponse.from(
                                                        saved, username));
                                    });
                        })
                );
    }

    public Flux<ApplicationResponse> getApplications(Long eventId, String username) {
        return resolveUserId(username)
                .flatMap(userId -> requireOwner(eventId, userId))
                .flatMapMany(e -> applicationRepository.findAllByEventId(eventId))
                .flatMap(app -> usernameOf(app.getUserId())
                        .map(uname -> ApplicationResponse.from(app, uname)));
    }

    public Mono<ApplicationResponse> reviewApplication(
            Long eventId, Long applicationId, StatusRequest req, String username) {
        if (!"ACCEPTED".equals(req.status()) && !"REJECTED".equals(req.status())) {
            return Mono.error(badRequest(
                    "Статус заявки должен быть ACCEPTED или REJECTED"));
        }
        return resolveUserId(username).flatMap(userId ->
                requireOwner(eventId, userId).flatMap(e ->
                        applicationRepository.findById(applicationId)
                                .switchIfEmpty(notFound("Заявка не найдена"))
                                .flatMap(app -> {
                                    if (!app.getEventId().equals(eventId)) {
                                        return Mono.error(badRequest(
                                                "Заявка не относится к этому мероприятию"));
                                    }
                                    app.setStatus(req.status());
                                    return applicationRepository.save(app)
                                            .flatMap(saved -> usernameOf(saved.getUserId())
                                                    .map(uname -> ApplicationResponse.from(
                                                            saved, uname)));
                                })));
    }



    private Mono<Boolean> checkAccess(Event e, Long userId) {
        if (e.getOwnerId().equals(userId)) return Mono.just(true);
        if ("OPEN".equals(e.getEventType())) return Mono.just(true);

        return invitationRepository.findByEventIdAndUserId(e.getId(), userId)
                .map(i -> "INVITED".equals(i.getStatus())
                        || "ACCEPTED".equals(i.getStatus()))
                .defaultIfEmpty(false)
                .flatMap(viaInv -> {
                    if (viaInv) return Mono.just(true);
                    return applicationRepository
                            .findByEventIdAndUserId(e.getId(), userId)
                            .map(a -> "ACCEPTED".equals(a.getStatus()))
                            .defaultIfEmpty(false);
                });
    }

    private Mono<Long> resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .switchIfEmpty(notFound("Пользователь не найден"));
    }

    private Mono<Event> requireOwner(Long eventId, Long userId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(notFound("Мероприятие не найдено"))
                .flatMap(e -> {
                    if (!e.getOwnerId().equals(userId)) {
                        return Mono.error(forbidden());
                    }
                    return Mono.just(e);
                });
    }

    private Mono<Void> validateDates(EventRequest req) {
        if (req.endsAt() != null && req.startsAt() != null
                && req.endsAt().isBefore(req.startsAt())) {
            return Mono.error(badRequest(
                    "Дата окончания не может быть раньше даты начала"));
        }
        return Mono.empty();
    }

    private <T> Mono<T> notFound(String msg) {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, msg));
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к мероприятию");
    }

    private ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
