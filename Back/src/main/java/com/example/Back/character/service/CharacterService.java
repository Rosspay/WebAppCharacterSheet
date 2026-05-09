package com.example.Back.character.service;

import com.example.Back.character.dto.*;
import com.example.Back.character.entity.Character;
import com.example.Back.character.entity.CharacterAccess;
import com.example.Back.character.repository.CharacterAccessRepository;
import com.example.Back.character.repository.CharacterRepository;
import com.example.Back.repository.UserRepository;
import com.example.Back.template.dto.PageResponse;
import com.example.Back.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final CharacterAccessRepository characterAccessRepository;
    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;

    public Flux<CharacterSummaryResponse> getMyCharacters(String username) {
        return resolveUserId(username)
                .flatMapMany(characterRepository::findAllByOwnerId)
                .map(CharacterSummaryResponse::from);
    }

    public Mono<PageResponse<CharacterSummaryResponse>> getAvailableCharacters(
            String query, int page, int size, String username) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return resolveUserId(username).flatMap(userId -> {
            Flux<Character> publicItems = (query != null && !query.isBlank())
                    ? characterRepository.searchPublic(query, pageable)
                    : characterRepository.findAllByVisibility("PUBLIC", pageable);
            Flux<Character> restrictedItems = characterRepository.findRestrictedForUser(userId);
            Flux<Character> merged = publicItems.mergeWith(restrictedItems)
                    .distinct(Character::getId);
            return Mono.zip(
                    merged.map(CharacterSummaryResponse::from).collectList(),
                    characterRepository.countByVisibility("PUBLIC")
            ).map(t -> PageResponse.of(t.getT1(), t.getT2(), page, size));
        });
    }

    public Mono<CharacterResponse> getById(Long id, String username) {
        return resolveUserId(username).flatMap(userId ->
                characterRepository.findById(id)
                        .switchIfEmpty(notFound("Character not found"))
                        .flatMap(c -> checkAccess(c, userId)
                                .flatMap(allowed -> {
                                    if (!allowed) return Mono.error(forbidden());
                                    return loadAllowedIds(c)
                                            .map(ids -> CharacterResponse.from(c, ids));
                                })
                        )
        );
    }

    public Mono<CharacterResponse> create(CharacterRequest req, String username) {
        return resolveUserId(username).flatMap(userId ->
                // Убеждаемся, что шаблон существует и доступен
                templateRepository.findById(req.templateId())
                        .switchIfEmpty(notFound("Template not found"))
                        .flatMap(template -> {
                            if (!template.isPublic() && !template.getOwnerId().equals(userId))
                                return Mono.error(forbidden());
                            Character c = Character.builder()
                                    .ownerId(userId)
                                    .templateId(req.templateId())
                                    .name(req.name())
                                    .description(req.description())
                                    .visibility(req.visibility() != null ? req.visibility() : "PRIVATE")
                                    .fieldValues(req.fieldValues())
                                    .build();
                            return characterRepository.save(c)
                                    .flatMap(saved -> syncAccessList(saved, req.allowedUserIds())
                                            .then(loadAllowedIds(saved))
                                            .map(ids -> CharacterResponse.from(saved, ids))
                                    );
                        })
        );
    }

    public Mono<CharacterResponse> update(Long id, CharacterRequest req, String username) {
        return resolveUserId(username).flatMap(userId ->
                requireOwner(id, userId).flatMap(c -> {
                    c.setName(req.name());
                    c.setDescription(req.description());
                    c.setFieldValues(req.fieldValues());
                    if (req.visibility() != null) c.setVisibility(req.visibility());
                    c.setUpdatedAt(LocalDateTime.now());
                    return characterRepository.save(c)
                            .flatMap(saved -> syncAccessList(saved, req.allowedUserIds())
                                    .then(loadAllowedIds(saved))
                                    .map(ids -> CharacterResponse.from(saved, ids))
                            );
                })
        );
    }

    public Mono<CharacterResponse> setVisibility(Long id, VisibilityRequest req, String username) {
        return resolveUserId(username).flatMap(userId ->
                requireOwner(id, userId).flatMap(c -> {
                    c.setVisibility(req.visibility());
                    c.setUpdatedAt(LocalDateTime.now());
                    return characterRepository.save(c)
                            .flatMap(saved -> syncAccessList(saved, req.allowedUserIds())
                                    .then(loadAllowedIds(saved))
                                    .map(ids -> CharacterResponse.from(saved, ids))
                            );
                })
        );
    }

    public Mono<Void> delete(Long id, String username) {
        return resolveUserId(username)
                .flatMap(userId -> requireOwner(id, userId))
                .flatMap(c -> characterAccessRepository.deleteAllByCharacterId(c.getId())
                        .then(characterRepository.deleteById(c.getId()))
                );
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Mono<Void> syncAccessList(Character c, List<Long> allowedUserIds) {
        return characterAccessRepository.deleteAllByCharacterId(c.getId())
                .then(
                        Flux.fromIterable(
                                        allowedUserIds != null && "RESTRICTED".equals(c.getVisibility())
                                                ? allowedUserIds
                                                : List.of()
                                )
                                .flatMap(uid -> characterAccessRepository.save(
                                        CharacterAccess.builder()
                                                .characterId(c.getId())
                                                .userId(uid)
                                                .build()
                                ))
                                .then()
                );
    }

    private Mono<List<Long>> loadAllowedIds(Character c) {
        return characterAccessRepository.findAllByCharacterId(c.getId())
                .map(CharacterAccess::getUserId)
                .collectList();
    }

    private Mono<Boolean> checkAccess(Character c, Long userId) {
        if (c.getOwnerId().equals(userId)) return Mono.just(true);
        return switch (c.getVisibility()) {
            case "PUBLIC" -> Mono.just(true);
            case "RESTRICTED" -> characterAccessRepository.existsByCharacterIdAndUserId(c.getId(), userId);
            default -> Mono.just(false); // PRIVATE
        };
    }

    private Mono<Long> resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .switchIfEmpty(notFound("User not found"));
    }

    private Mono<Character> requireOwner(Long characterId, Long userId) {
        return characterRepository.findById(characterId)
                .switchIfEmpty(notFound("Character not found"))
                .flatMap(c -> {
                    if (!c.getOwnerId().equals(userId)) return Mono.error(forbidden());
                    return Mono.just(c);
                });
    }

    private <T> Mono<T> notFound(String msg) {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, msg));
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
}
