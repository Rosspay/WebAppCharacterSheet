package com.example.Back.service;

import com.example.Back.dto.*;
import com.example.Back.entity.RefreshToken;
import com.example.Back.entity.User;
import com.example.Back.repository.RefreshTokenRepository;
import com.example.Back.repository.UserRepository;
import com.example.Back.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public Mono<TokenResponse> register(RegisterRequest request) {
        return userRepository.existsByUsername(request.username())
                .flatMap(usernameExists -> {
                    if (usernameExists) return Mono.error(
                            new IllegalArgumentException("Username already taken"));
                    return userRepository.existsByEmail(request.email());
                })
                .flatMap(emailExists -> {
                    if (emailExists) return Mono.error(
                            new IllegalArgumentException("Email already registered"));
                    User user = User.builder()
                            .username(request.username())
                            .email(request.email())
                            .password(passwordEncoder.encode(request.password()))
                            .build();
                    return userRepository.save(user);
                })
                .flatMap(this::issueTokens);
    }

    public Mono<TokenResponse> login(LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid credentials")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                        return Mono.error(new BadCredentialsException("Invalid credentials"));
                    }
                    if (!user.isEnabled()) {
                        return Mono.error(new BadCredentialsException("Account disabled"));
                    }
                    return issueTokens(user);
                });
    }

    @Transactional
    public Mono<TokenResponse> refresh(String rawRefreshToken) {
        return refreshTokenRepository.findByToken(rawRefreshToken)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Refresh token not found")))
                .flatMap(token -> {
                    if (token.isRevoked()) {
                        return Mono.error(new BadCredentialsException("Refresh token revoked"));
                    }
                    if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return Mono.error(new BadCredentialsException("Refresh token expired"));
                    }
                    // Revoke used token (rotation)
                    token.setRevoked(true);
                    return refreshTokenRepository.save(token)
                            .flatMap(t -> userRepository.findById(t.getUserId()));
                })
                .flatMap(this::issueTokens);
    }

    @Transactional
    public Mono<Void> logout(Long userId) {
        return refreshTokenRepository.deleteAllByUserId(userId);
    }

    public Mono<UserResponse> getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::from);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Mono<TokenResponse> issueTokens(User user) {
        String accessToken  = jwtService.generateAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        RefreshToken entity = RefreshToken.builder()
                .token(refreshToken)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtService.getRefreshTokenExpiration() / 1000))
                .build();

        return refreshTokenRepository.save(entity)
                .map(saved -> TokenResponse.of(
                        accessToken, refreshToken,
                        jwtService.getAccessTokenExpiration()));
    }
}
