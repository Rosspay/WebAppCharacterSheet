package com.example.Back.character.repository;

import com.example.Back.character.entity.CharacterAccess;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * Reactive repository for character access rules.
 */

public interface CharacterAccessRepository extends ReactiveCrudRepository<CharacterAccess, Long> {
    Flux<CharacterAccess> findAllByCharacterId(Long characterId);
    Mono<Boolean> existsByCharacterIdAndUserId(Long characterId, Long userId);
    Mono<Void> deleteAllByCharacterId(Long characterId);
    Mono<Void> deleteByCharacterIdAndUserId(Long characterId, Long userId);
}
