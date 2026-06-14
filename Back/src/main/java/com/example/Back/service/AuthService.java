package com.example.Back.service;

import com.example.Back.dto.*;
import com.example.Back.entity.RefreshToken;
import com.example.Back.entity.User;
import com.example.Back.repository.RefreshTokenRepository;
import com.example.Back.repository.UserRepository;
import com.example.Back.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
/**
 * Core authentication service: registration, login, refresh-token rotation and Yandex OAuth sign-in.
 */

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final YandexOAuthService yandexOAuthService;

    /**
     * Registers a new user with a username/password pair.
     *
     * <p>The username and e-mail must be globally unique; if either is already
     * taken an {@link IllegalArgumentException} is emitted. The password is
     * hashed with the configured {@link PasswordEncoder} before persistence.
     * On success a fresh access/refresh token pair is issued via
     * {@link #issueTokens(User)}.
     *
     * @param request validated registration payload
     * @return token pair for the freshly created account
     */
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

    /**
     * Authenticates a user by username and raw password.
     *
     * <p>Returns a generic {@link BadCredentialsException} for any failure
     * (unknown user, wrong password, disabled account) so the response does not
     * leak which factor was incorrect.
     *
     * @param request login payload
     * @return token pair on success
     */
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

    /**
     * Rotates a refresh token: the supplied token is invalidated and a fresh
     * access/refresh pair is issued for the same user.
     *
     * <p>Implements one-time-use semantics — the old refresh token is marked
     * revoked in the same transaction that issues the new one. If the token is
     * unknown, already revoked or expired, a {@link BadCredentialsException} is
     * emitted and no new tokens are produced.
     *
     * @param rawRefreshToken signed refresh token sent by the client
     * @return new access/refresh token pair
     */
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




    /**
     * Signs the caller in via Yandex OAuth.
     *
     * <p>The authorization code is exchanged for a Yandex access token, the
     * Yandex user profile is fetched and resolved to a local account using
     * {@link #upsertYandexUser(YandexUserInfo)}. A fresh access/refresh token
     * pair is then issued.
     *
     * @param code        authorization code returned by Yandex
     * @param redirectUri redirect URI used during the authorization request (must match)
     * @return token pair on success
     */
    @Transactional
    public Mono<TokenResponse> loginWithYandex(String code, String redirectUri) {
        return yandexOAuthService.exchangeCodeForToken(code, redirectUri)
                .flatMap(token -> yandexOAuthService.fetchUserInfo(token.accessToken()))
                .flatMap(this::upsertYandexUser)
                .flatMap(this::issueTokens);
    }

    /**
     * Resolves a Yandex profile to a local {@link User}, creating or linking
     * the account on the fly.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>look up by {@code yandex_id};</li>
     *   <li>if missing, fall back to {@link #linkOrCreateForYandex} (link by
     *       default e-mail, otherwise create a new account);</li>
     *   <li>if a concurrent callback wins the {@code users_username_key} race
     *       and triggers a {@link DuplicateKeyException}, re-read by
     *       {@code yandex_id} and return the winner so the loser does not 500.</li>
     * </ol>
     *
     * @param info Yandex profile (id is required)
     * @return resolved local user
     */
    Mono<User> upsertYandexUser(YandexUserInfo info) {
        if (info == null || info.id() == null || info.id().isBlank()) {
            return Mono.error(new BadCredentialsException(
                    "Не удалось получить идентификатор пользователя Яндекса"));
        }
        return userRepository.findByYandexId(info.id())
                .switchIfEmpty(Mono.defer(() -> linkOrCreateForYandex(info)))



                .onErrorResume(DuplicateKeyException.class,
                        e -> userRepository.findByYandexId(info.id())
                                .switchIfEmpty(Mono.error(e)));
    }


    /**
     * Links the Yandex profile to an existing account when an e-mail match
     * exists, otherwise creates a brand new local account via
     * {@link #createUserFromYandex(YandexUserInfo)}.
     */
    private Mono<User> linkOrCreateForYandex(YandexUserInfo info) {
        if (info.defaultEmail() == null || info.defaultEmail().isBlank()) {
            return createUserFromYandex(info);
        }
        return userRepository.findByEmail(info.defaultEmail())
                .flatMap(existing -> {
                    existing.setYandexId(info.id());
                    return userRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> createUserFromYandex(info)));
    }

    /**
     * Creates a new local user from a Yandex profile.
     *
     * <p>The username is derived from {@code info.login()} (or {@code ya_<id>}
     * when the login is missing) and made unique via
     * {@link #ensureUniqueUsername(String)}. If the Yandex e-mail collides with
     * an existing local account, a synthetic {@code <id>+<uuid>@yandex.local}
     * e-mail is used to avoid the unique-key violation.
     */
    private Mono<User> createUserFromYandex(YandexUserInfo info) {
        String baseUsername = info.login() != null && !info.login().isBlank()
                ? info.login()
                : "ya_" + info.id();

        String email = (info.defaultEmail() != null && !info.defaultEmail().isBlank())
                ? info.defaultEmail()
                : info.id() + "@yandex.local";

        return ensureUniqueUsername(baseUsername).flatMap(username ->
                userRepository.existsByEmail(email).flatMap(emailExists -> {
                    String finalEmail = emailExists
                            ? info.id() + "+" + UUID.randomUUID() + "@yandex.local"
                            : email;
                    User user = User.builder()
                            .username(username)
                            .email(finalEmail)
                            .password(null)
                            .role("ROLE_USER")
                            .enabled(true)
                            .provider("YANDEX")
                            .yandexId(info.id())
                            .build();
                    return userRepository.save(user);
                }));
    }

    private Mono<String> ensureUniqueUsername(String base) {
        return userRepository.existsByUsername(base)
                .flatMap(exists -> exists
                        ? Mono.just(base + "_" + UUID.randomUUID().toString().substring(0, 6))
                        : Mono.just(base));
    }



    /**
     * Issues a fresh access/refresh token pair for {@code user} and persists
     * the refresh token with an expiration timestamp derived from the JWT
     * configuration.
     */
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
