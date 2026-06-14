package com.example.Back.event.service;

import com.example.Back.event.dto.ApplicationRequest;
import com.example.Back.event.dto.EventRequest;
import com.example.Back.event.dto.InviteRequest;
import com.example.Back.event.dto.StatusRequest;
import com.example.Back.event.entity.Event;
import com.example.Back.event.entity.EventApplication;
import com.example.Back.event.entity.EventInvitation;
import com.example.Back.event.repository.EventApplicationRepository;
import com.example.Back.event.repository.EventInvitationRepository;
import com.example.Back.event.repository.EventRepository;
import com.example.Back.entity.User;
import com.example.Back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventInvitationRepository invitationRepository;
    @Mock private EventApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, invitationRepository,
                applicationRepository, userRepository);
        lenient().when(userRepository.findByUsername("owner"))
                .thenReturn(Mono.just(User.builder().id(1L).username("owner").build()));
        lenient().when(userRepository.findByUsername("guest"))
                .thenReturn(Mono.just(User.builder().id(2L).username("guest").build()));
        lenient().when(userRepository.findByUsername("stranger"))
                .thenReturn(Mono.just(User.builder().id(3L).username("stranger").build()));
        lenient().when(userRepository.findById(anyLong()))
                .thenAnswer(inv -> Mono.just(User.builder()
                        .id(inv.getArgument(0, Long.class))
                        .username("u" + inv.getArgument(0))
                        .build()));
    }

    private EventRequest validRequest() {
        return new EventRequest(
                "Title",
                "desc",
                "loc",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "CLOSED",
                false
        );
    }

    private Event closedOwnedByOwner() {
        return Event.builder()
                .id(10L).ownerId(1L)
                .title("E").description("d").location("l")
                .startsAt(LocalDateTime.now().plusDays(1))
                .endsAt(LocalDateTime.now().plusDays(2))
                .eventType("CLOSED")
                .allowApplications(false)
                .build();
    }

    private Event openOwnedByOwner(boolean allowApps) {
        Event e = closedOwnedByOwner();
        e.setEventType("OPEN");
        e.setAllowApplications(allowApps);
        return e;
    }



    @Test
    @DisplayName("UT-B-40: create отклоняет конечную дату раньше начальной")
    void create_rejectsEndBeforeStart() {
        var req = new EventRequest("T", null, null,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1),
                "CLOSED", false);

        StepVerifier.create(eventService.create(req, "owner"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 400)
                .verify();
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-B-41: create happy-path сохраняет и возвращает EventResponse")
    void create_happyPath() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0, Event.class);
            e.setId(7L);
            return Mono.just(e);
        });

        StepVerifier.create(eventService.create(validRequest(), "owner"))
                .assertNext(resp -> {
                    org.assertj.core.api.Assertions.assertThat(resp.id()).isEqualTo(7L);
                    org.assertj.core.api.Assertions.assertThat(resp.title()).isEqualTo("Title");
                    org.assertj.core.api.Assertions.assertThat(resp.ownerId()).isEqualTo(1L);
                })
                .verifyComplete();
    }



    @Test
    @DisplayName("UT-B-42: getById возвращает 403 чужому пользователю на CLOSED событии")
    void getById_closedEvent_forbiddenForStranger() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(closedOwnedByOwner()));
        when(invitationRepository.findByEventIdAndUserId(10L, 3L)).thenReturn(Mono.empty());
        when(applicationRepository.findByEventIdAndUserId(10L, 3L)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.getById(10L, "stranger"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 403)
                .verify();
    }

    @Test
    @DisplayName("UT-B-43: getById пускает приглашённого на CLOSED событие")
    void getById_closedEvent_allowsInvitedUser() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(closedOwnedByOwner()));
        when(invitationRepository.findByEventIdAndUserId(10L, 2L))
                .thenReturn(Mono.just(EventInvitation.builder()
                        .id(99L).eventId(10L).userId(2L).status("INVITED").build()));

        StepVerifier.create(eventService.getById(10L, "guest"))
                .assertNext(r -> org.assertj.core.api.Assertions.assertThat(r.id()).isEqualTo(10L))
                .verifyComplete();
    }

    @Test
    @DisplayName("UT-B-44: getById пускает любого на OPEN событие")
    void getById_openEvent_allowsAnyAuthenticated() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(openOwnedByOwner(false)));

        StepVerifier.create(eventService.getById(10L, "stranger"))
                .assertNext(r -> org.assertj.core.api.Assertions.assertThat(r.id()).isEqualTo(10L))
                .verifyComplete();
    }



    @Test
    @DisplayName("UT-B-45: update только для владельца — 403 чужому")
    void update_forbiddenForNonOwner() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(closedOwnedByOwner()));

        StepVerifier.create(eventService.update(10L, validRequest(), "stranger"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 403)
                .verify();
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-B-46: delete только для владельца — каскадно чистит приглашения и заявки")
    void delete_ownerOnly_andCascades() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(closedOwnedByOwner()));
        when(invitationRepository.deleteAllByEventId(10L)).thenReturn(Mono.empty());
        when(applicationRepository.deleteAllByEventId(10L)).thenReturn(Mono.empty());
        when(eventRepository.deleteById(10L)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.delete(10L, "owner")).verifyComplete();
        verify(invitationRepository).deleteAllByEventId(10L);
        verify(applicationRepository).deleteAllByEventId(10L);
        verify(eventRepository).deleteById(10L);
    }



    @Test
    @DisplayName("UT-B-47: invite — приглашение самому себе запрещено (400)")
    void invite_selfInviteRejected() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(closedOwnedByOwner()));

        StepVerifier.create(eventService.invite(10L, new InviteRequest("owner"), "owner"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 400)
                .verify();
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-B-48: invite — дубликат приглашения отклоняется (400)")
    void invite_duplicateRejected() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(closedOwnedByOwner()));
        when(invitationRepository.existsByEventIdAndUserId(10L, 2L)).thenReturn(Mono.just(true));

        StepVerifier.create(eventService.invite(10L, new InviteRequest("guest"), "owner"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 400)
                .verify();
    }

    @Test
    @DisplayName("UT-B-49: invite happy-path сохраняет приглашение")
    void invite_happyPath() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(closedOwnedByOwner()));
        when(invitationRepository.existsByEventIdAndUserId(10L, 2L)).thenReturn(Mono.just(false));
        when(invitationRepository.save(any(EventInvitation.class)))
                .thenAnswer(inv -> {
                    EventInvitation e = inv.getArgument(0);
                    e.setId(33L);
                    return Mono.just(e);
                });

        StepVerifier.create(eventService.invite(10L, new InviteRequest("guest"), "owner"))
                .assertNext(r -> {
                    org.assertj.core.api.Assertions.assertThat(r.id()).isEqualTo(33L);
                    org.assertj.core.api.Assertions.assertThat(r.status()).isEqualTo("INVITED");
                    org.assertj.core.api.Assertions.assertThat(r.username()).isEqualTo("guest");
                })
                .verifyComplete();
    }



    @Test
    @DisplayName("UT-B-50: respondToInvitation — нельзя отвечать за другого (403)")
    void respondToInvitation_otherUser_forbidden() {
        EventInvitation inv = EventInvitation.builder()
                .id(99L).eventId(10L).userId(2L).status("INVITED").build();
        when(invitationRepository.findById(99L)).thenReturn(Mono.just(inv));

        StepVerifier.create(eventService.respondToInvitation(99L,
                        new StatusRequest("ACCEPTED"), "stranger"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 403)
                .verify();
    }

    @Test
    @DisplayName("UT-B-51: respondToInvitation — статус ACCEPTED обновляет приглашение")
    void respondToInvitation_acceptsCorrectly() {
        EventInvitation inv = EventInvitation.builder()
                .id(99L).eventId(10L).userId(2L).status("INVITED").build();
        when(invitationRepository.findById(99L)).thenReturn(Mono.just(inv));
        when(invitationRepository.save(any(EventInvitation.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(eventService.respondToInvitation(99L,
                        new StatusRequest("ACCEPTED"), "guest"))
                .assertNext(r -> {
                    org.assertj.core.api.Assertions.assertThat(r.status()).isEqualTo("ACCEPTED");
                    org.assertj.core.api.Assertions.assertThat(r.username()).isEqualTo("guest");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("UT-B-52: respondToInvitation — неизвестный статус → 400")
    void respondToInvitation_badStatus() {
        StepVerifier.create(eventService.respondToInvitation(99L,
                        new StatusRequest("WAT"), "guest"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 400)
                .verify();
    }



    @Test
    @DisplayName("UT-B-53: apply — заявка к CLOSED событию запрещена")
    void apply_toClosedEvent_rejected() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(closedOwnedByOwner()));

        StepVerifier.create(eventService.apply(10L,
                        new ApplicationRequest("hi"), "stranger"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 400)
                .verify();
    }

    @Test
    @DisplayName("UT-B-54: apply — owner не может подать заявку (400)")
    void apply_byOwner_rejected() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(openOwnedByOwner(true)));

        StepVerifier.create(eventService.apply(10L, new ApplicationRequest("x"), "owner"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode().value() == 400)
                .verify();
    }

    @Test
    @DisplayName("UT-B-55: apply happy-path сохраняет заявку")
    void apply_happyPath() {
        when(eventRepository.findById(10L)).thenReturn(Mono.just(openOwnedByOwner(true)));
        when(applicationRepository.existsByEventIdAndUserId(10L, 3L)).thenReturn(Mono.just(false));
        when(applicationRepository.save(any(EventApplication.class)))
                .thenAnswer(inv -> {
                    EventApplication a = inv.getArgument(0);
                    a.setId(77L);
                    return Mono.just(a);
                });

        StepVerifier.create(eventService.apply(10L,
                        new ApplicationRequest("Хочу участвовать"), "stranger"))
                .assertNext(r -> {
                    org.assertj.core.api.Assertions.assertThat(r.id()).isEqualTo(77L);
                    org.assertj.core.api.Assertions.assertThat(r.status()).isEqualTo("PENDING");
                })
                .verifyComplete();
    }
}
