package com.example.Back.character.controller;

import com.example.Back.character.dto.*;
import com.example.Back.character.service.CharacterService;
import com.example.Back.template.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping("/my")
    public Flux<CharacterSummaryResponse> getMyCharacters(
            @AuthenticationPrincipal UserDetails user) {
        return characterService.getMyCharacters(user.getUsername());
    }

    @GetMapping("/available")
    public Mono<PageResponse<CharacterSummaryResponse>> getAvailableCharacters(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {
        return characterService.getAvailableCharacters(query, page, size, user.getUsername());
    }

    @GetMapping("/{id}")
    public Mono<CharacterResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return characterService.getById(id, user.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CharacterResponse> create(
            @Valid @RequestBody CharacterRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return characterService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    public Mono<CharacterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CharacterRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return characterService.update(id, request, user.getUsername());
    }

    @PatchMapping("/{id}/visibility")
    public Mono<CharacterResponse> setVisibility(
            @PathVariable Long id,
            @Valid @RequestBody VisibilityRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return characterService.setVisibility(id, request, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return characterService.delete(id, user.getUsername());
    }
}
