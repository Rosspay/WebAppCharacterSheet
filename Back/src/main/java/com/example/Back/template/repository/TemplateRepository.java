package com.example.Back.template.repository;

import com.example.Back.template.entity.Template;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/**
 * Reactive repository for templates with pagination and title search.
 */

public interface TemplateRepository extends ReactiveCrudRepository<Template, Long> {

    Flux<Template> findAllByOwnerId(Long ownerId);

    Flux<Template> findAllByIsPublicTrue(Pageable pageable);

    Mono<Long> countByIsPublicTrue();

    @Query("SELECT * FROM templates WHERE is_public = TRUE AND " +
            "(LOWER(title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Flux<Template> searchPublic(String query, Pageable pageable);

    Mono<Boolean> existsByIdAndOwnerId(Long id, Long ownerId);
}
