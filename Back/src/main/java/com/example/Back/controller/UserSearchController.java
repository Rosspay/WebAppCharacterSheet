package com.example.Back.controller;

import com.example.Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
/**
 * Username search endpoint used by the UI autocomplete when inviting users to events or granting character access.
 */


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserSearchController {

    private static final int MAX_LIMIT = 20;

    private final UserRepository userRepository;


    /**
     * Returns a list of usernames matching the case-insensitive prefix.
     *
     * <p>Used by the autocomplete on the invitation and access-grant forms.
     * The caller is excluded from the result so users do not invite/grant
     * themselves. The {@code limit} parameter is clamped to {@code [1, 20]} to
     * keep result sizes bounded; an empty/blank {@code q} returns an empty
     * list without hitting the database.
     *
     * @param q     username prefix (trimmed; case-insensitive)
     * @param limit maximum number of usernames to return (clamped to 1..20)
     * @param user  current authenticated principal (excluded from results)
     * @return up to {@code limit} matching usernames
     */
    @GetMapping("/search")
    public Mono<List<String>> search(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails user) {
        String prefix = q == null ? "" : q.trim();
        if (prefix.isEmpty()) return Mono.just(List.of());

        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        return userRepository.findByUsername(user.getUsername())
                .flatMapMany(me -> userRepository.searchByUsernamePrefix(
                        prefix, me.getId(), safeLimit))
                .map(u -> u.getUsername())
                .collectList();
    }
}
