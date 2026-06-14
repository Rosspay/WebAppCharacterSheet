package com.example.Back.template.controller;

import com.example.Back.template.dto.*;
import com.example.Back.template.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * REST endpoints for character-sheet templates: CRUD, listing, pagination and search.
 */

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping("/my")
    public Flux<TemplateSummaryResponse> getMyTemplates(
            @AuthenticationPrincipal UserDetails user) {
        return templateService.getMyTemplates(user.getUsername());
    }

    @GetMapping("/public")
    public Mono<PageResponse<TemplateSummaryResponse>> getPublicTemplates(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return templateService.getPublicTemplates(query, page, size);
    }

    @GetMapping("/{id}")
    public Mono<TemplateResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return templateService.getById(id, user.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TemplateResponse> create(
            @Valid @RequestBody TemplateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return templateService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    public Mono<TemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TemplateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return templateService.update(id, request, user.getUsername());
    }

    @PatchMapping("/{id}/publish")
    public Mono<TemplateResponse> togglePublish(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return templateService.togglePublish(id, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return templateService.delete(id, user.getUsername());
    }
}