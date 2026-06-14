package com.example.Back.repository;

import com.example.Back.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * Reactive repository for {@link com.example.Back.entity.User} including a case-insensitive prefix search used by the autocomplete endpoint.
 */

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUsername(String username);
    Mono<User> findByEmail(String email);
    Mono<User> findByYandexId(String yandexId);
    Mono<Boolean> existsByUsername(String username);
    Mono<Boolean> existsByEmail(String email);

    @Query("SELECT * FROM users " +
           "WHERE LOWER(username) LIKE LOWER(:prefix) || '%' " +
           "  AND id <> :excludeId " +
           "ORDER BY username " +
           "LIMIT :limit")
    Flux<User> searchByUsernamePrefix(String prefix, Long excludeId, int limit);
}