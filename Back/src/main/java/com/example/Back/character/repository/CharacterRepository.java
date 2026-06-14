package com.example.Back.character.repository;

import com.example.Back.character.entity.Character;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * Reactive repository for {@link com.example.Back.character.entity.Character}.
 */

public interface CharacterRepository extends ReactiveCrudRepository<Character, Long> {

    Flux<Character> findAllByOwnerId(Long ownerId);

    Flux<Character> findAllByVisibility(String visibility, Pageable pageable);

    Mono<Long> countByVisibility(String visibility);

    @Query("SELECT * FROM characters WHERE visibility = 'PUBLIC' " +
            "AND (LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Flux<Character> searchPublic(String query, Pageable pageable);

    @Query("SELECT c.* FROM characters c " +
            "JOIN character_access ca ON ca.character_id = c.id " +
            "WHERE ca.user_id = :userId AND c.visibility = 'RESTRICTED'")
    Flux<Character> findRestrictedForUser(Long userId);

    Mono<Boolean> existsByIdAndOwnerId(Long id, Long ownerId);
}
