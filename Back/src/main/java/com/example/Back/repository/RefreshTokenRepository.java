package com.example.Back.repository;

import com.example.Back.entity.RefreshToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {
    Mono<RefreshToken> findByToken(String token);
    Mono<Void> deleteAllByUserId(Long userId);
}
