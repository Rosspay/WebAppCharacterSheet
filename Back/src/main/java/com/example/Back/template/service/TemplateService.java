package com.example.Back.template.service;

import com.example.Back.template.dto.*;
import com.example.Back.template.entity.Template;
import com.example.Back.template.repository.TemplateRepository;
import com.example.Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;

    public Flux<TemplateSummaryResponse> getMyTemplates(String username) {
        return resolveUserId(username)
                .flatMapMany(templateRepository::findAllByOwnerId)
                .map(TemplateSummaryResponse::from);
    }

    public Mono<PageResponse<TemplateSummaryResponse>> getPublicTemplates(
            String query, int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Flux<Template> items = (query != null && !query.isBlank())
                ? templateRepository.searchPublic(query, pageable)
                : templateRepository.findAllByIsPublicTrue(pageable);

        return Mono.zip(
                items.map(TemplateSummaryResponse::from).collectList(),
                templateRepository.countByIsPublicTrue()
        ).map(t -> PageResponse.of(t.getT1(), t.getT2(), page, size));
    }

    public Mono<TemplateResponse> getById(Long id, String username) {
        return resolveUserId(username).flatMap(userId ->
                templateRepository.findById(id)
                        .switchIfEmpty(notFound("Шаблон не найден"))
                        .flatMap(t -> {
                            if (!t.isPublic() && !t.getOwnerId().equals(userId)) {
                                return Mono.error(new ResponseStatusException(
                                        HttpStatus.FORBIDDEN, "Нет доступа к шаблону"));
                            }
                            return Mono.just(TemplateResponse.from(t));
                        })
        );
    }

    public Mono<TemplateResponse> create(TemplateRequest req, String username) {
        return resolveUserId(username).flatMap(userId -> {
            Template t = Template.builder()
                    .ownerId(userId)
                    .title(req.title())
                    .description(req.description())
                    .content(req.content())
                    .build();
            return templateRepository.save(t).map(TemplateResponse::from);
        });
    }

    public Mono<TemplateResponse> update(Long id, TemplateRequest req, String username) {
        return resolveUserId(username).flatMap(userId ->
                requireOwner(id, userId).flatMap(t -> {
                    t.setTitle(req.title());
                    t.setDescription(req.description());
                    t.setContent(req.content());
                    t.setUpdatedAt(LocalDateTime.now());
                    return templateRepository.save(t).map(TemplateResponse::from);
                })
        );
    }

    public Mono<TemplateResponse> togglePublish(Long id, String username) {
        return resolveUserId(username).flatMap(userId ->
                requireOwner(id, userId).flatMap(t -> {
                    t.setPublic(!t.isPublic());
                    t.setUpdatedAt(LocalDateTime.now());
                    return templateRepository.save(t).map(TemplateResponse::from);
                })
        );
    }

    public Mono<Void> delete(Long id, String username) {
        return resolveUserId(username)
                .flatMap(userId -> requireOwner(id, userId))
                .flatMap(t -> templateRepository.deleteById(t.getId()));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Mono<Long> resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .switchIfEmpty(notFound("Пользователь не найден"));
    }

    private Mono<Template> requireOwner(Long templateId, Long userId) {
        return templateRepository.findById(templateId)
                .switchIfEmpty(notFound("Шаблон не найден"))
                .flatMap(t -> {
                    if (!t.getOwnerId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "Нет прав на изменение шаблона"));
                    }
                    return Mono.just(t);
                });
    }

    private <T> Mono<T> notFound(String msg) {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, msg));
    }
}
