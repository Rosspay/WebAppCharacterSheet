package com.example.Back.security;

import com.example.Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
/**
 * Reactive UserDetailsService backed by {@link com.example.Back.repository.UserRepository}.
 */

@Service
@RequiredArgsConstructor
public class ReactiveUserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities(List.of(new SimpleGrantedAuthority(user.getRole())))
                        .accountExpired(false)
                        .accountLocked(!user.isEnabled())
                        .credentialsExpired(false)
                        .disabled(!user.isEnabled())
                        .build()
                );
    }
}