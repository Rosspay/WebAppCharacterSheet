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
/**
 * Service managing characters and access rules.
 */

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final CharacterAccessRepository characterAccessRepository;
    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;
    private final CharacterPdfService characterPdfService;

    public Flux<CharacterSummaryResponse> getMyCharacters(String username) {
        return resolveUserId(username)
                .flatMapMany(characterRepository::findAllByOwnerId)
                .map(CharacterSummaryResponse::from);
    }

    /**
     * Returns the page of characters visible to the caller in the public feed.
     *
     * <p>The merge is a union of two streams:
     * <ul>
     *   <li>characters with {@code visibility = PUBLIC} matching the optional
     *       title search;</li>
     *   <li>characters with {@code visibility = RESTRICTED} where the caller is
     *       explicitly granted access via {@code character_access}.</li>
     * </ul>
     * The streams are deduplicated by id so a restricted character that also
     * matches a public search does not appear twice. The {@code total} count is
     * approximated by the number of public items so that page navigation
     * remains stable when restricted grants change.
     *
     * @param query    optional title fragment
     * @param page     0-based page index
     * @param size     page size
     * @param username caller's username
     * @return paginated summary DTOs
     */
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
                                    return loadAllowedUsernames(c)
                                            .map(usernames -> CharacterResponse.from(c, usernames));
                                })
                        )
        );
    }

    public Mono<CharacterResponse> create(CharacterRequest req, String username) {
        return resolveUserId(username).flatMap(userId ->

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
                                    .flatMap(saved -> syncAccessList(saved, req.allowedUsernames())
                                            .then(loadAllowedUsernames(saved))
                                            .map(usernames -> CharacterResponse.from(saved, usernames))
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
                            .flatMap(saved -> syncAccessList(saved, req.allowedUsernames())
                                    .then(loadAllowedUsernames(saved))
                                    .map(usernames -> CharacterResponse.from(saved, usernames))
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
                            .flatMap(saved -> syncAccessList(saved, req.allowedUsernames())
                                    .then(loadAllowedUsernames(saved))
                                    .map(usernames -> CharacterResponse.from(saved, usernames))
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


    public Mono<byte[]> exportPdf(Long id, String username) {
        return resolveUserId(username).flatMap(userId ->
                characterRepository.findById(id)
                        .switchIfEmpty(notFound("Character not found"))
                        .flatMap(c -> checkAccess(c, userId).flatMap(allowed -> {
                            if (!allowed) return Mono.error(forbidden());
                            return templateRepository.findById(c.getTemplateId())
                                    .map(t -> characterPdfService.generate(c, t))

                                    .switchIfEmpty(Mono.fromCallable(
                                            () -> characterPdfService.generate(c, null)));
                        }))
        );
    }




    /**
     * Replaces the access-control list for a character with the given
     * usernames.
     *
     * <p>Existing {@code character_access} rows are deleted unconditionally;
     * new rows are inserted only when the character is RESTRICTED (for PUBLIC
     * and PRIVATE characters the list is ignored). Unknown usernames in the
     * input are silently skipped so the operation is idempotent and resilient
     * to UI typos.
     *
     * @param c                target character
     * @param allowedUsernames list of usernames; may be {@code null}
     */
    private Mono<Void> syncAccessList(Character c, List<String> allowedUsernames) {
        return characterAccessRepository.deleteAllByCharacterId(c.getId())
                .then(
                        Flux.fromIterable(
                                        allowedUsernames != null
                                                && "RESTRICTED".equals(c.getVisibility())
                                                ? allowedUsernames
                                                : List.of()
                                )
                                .flatMap(name -> userRepository.findByUsername(name)
                                        .flatMap(u -> characterAccessRepository.save(
                                                CharacterAccess.builder()
                                                        .characterId(c.getId())
                                                        .userId(u.getId())
                                                        .build()))
                                )
                                .then()
                );
    }

    private Mono<List<String>> loadAllowedUsernames(Character c) {
        return characterAccessRepository.findAllByCharacterId(c.getId())
                .map(CharacterAccess::getUserId)
                .flatMap(uid -> userRepository.findById(uid).map(u -> u.getUsername()))
                .collectList();
    }

    /**
     * Resolves whether {@code userId} may read a given character.
     *
     * <p>Owners always have access. PUBLIC characters are world-readable.
     * RESTRICTED characters require an explicit row in
     * {@code character_access}. PRIVATE characters are visible to the owner
     * only.
     */
    private Mono<Boolean> checkAccess(Character c, Long userId) {
        if (c.getOwnerId().equals(userId)) return Mono.just(true);
        return switch (c.getVisibility()) {
            case "PUBLIC" -> Mono.just(true);
            case "RESTRICTED" -> characterAccessRepository.existsByCharacterIdAndUserId(c.getId(), userId);
            default -> Mono.just(false);
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
