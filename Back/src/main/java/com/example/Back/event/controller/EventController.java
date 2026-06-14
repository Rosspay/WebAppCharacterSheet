package com.example.Back.event.controller;

import com.example.Back.event.dto.*;
import com.example.Back.event.service.EventService;
import com.example.Back.template.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * REST endpoints for events: CRUD, invitations, applications and status changes.
 */

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;



    @GetMapping("/my")
    public Flux<EventSummaryResponse> getMyEvents(
            @AuthenticationPrincipal UserDetails user) {
        return eventService.getMyEvents(user.getUsername());
    }

    @GetMapping("/participating")
    public Flux<EventSummaryResponse> getParticipating(
            @AuthenticationPrincipal UserDetails user) {
        return eventService.getParticipatingEvents(user.getUsername());
    }

    @GetMapping("/open")
    public Mono<PageResponse<EventSummaryResponse>> getOpenEvents(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return eventService.getOpenEvents(query, page, size);
    }

    @GetMapping("/{id}")
    public Mono<EventResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.getById(id, user.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EventResponse> create(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    public Mono<EventResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.update(id, request, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.delete(id, user.getUsername());
    }



    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<InvitationResponse> invite(
            @PathVariable("id") Long eventId,
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.invite(eventId, request, user.getUsername());
    }

    @GetMapping("/{id}/invitations")
    public Flux<InvitationResponse> getInvitations(
            @PathVariable("id") Long eventId,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.getInvitations(eventId, user.getUsername());
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> cancelInvitation(
            @PathVariable("id") Long eventId,
            @PathVariable Long invitationId,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.cancelInvitation(eventId, invitationId, user.getUsername());
    }

    @GetMapping("/invitations/my")
    public Flux<InvitationResponse> getMyInvitations(
            @AuthenticationPrincipal UserDetails user) {
        return eventService.getMyInvitations(user.getUsername());
    }

    @PatchMapping("/invitations/{invitationId}")
    public Mono<InvitationResponse> respondToInvitation(
            @PathVariable Long invitationId,
            @Valid @RequestBody StatusRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.respondToInvitation(invitationId, request, user.getUsername());
    }



    @PostMapping("/{id}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApplicationResponse> apply(
            @PathVariable("id") Long eventId,
            @Valid @RequestBody(required = false) ApplicationRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.apply(eventId, request, user.getUsername());
    }

    @GetMapping("/{id}/applications")
    public Flux<ApplicationResponse> getApplications(
            @PathVariable("id") Long eventId,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.getApplications(eventId, user.getUsername());
    }

    @PatchMapping("/{id}/applications/{applicationId}")
    public Mono<ApplicationResponse> reviewApplication(
            @PathVariable("id") Long eventId,
            @PathVariable Long applicationId,
            @Valid @RequestBody StatusRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.reviewApplication(
                eventId, applicationId, request, user.getUsername());
    }
}
