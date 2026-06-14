package com.example.Back.service;

import com.example.Back.dto.LoginRequest;
import com.example.Back.dto.RegisterRequest;
import com.example.Back.dto.TokenResponse;
import com.example.Back.dto.YandexTokenResponse;
import com.example.Back.dto.YandexUserInfo;
import com.example.Back.entity.RefreshToken;
import com.example.Back.entity.User;
import com.example.Back.repository.RefreshTokenRepository;
import com.example.Back.repository.UserRepository;
import com.example.Back.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private YandexOAuthService yandexOAuthService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository,
                passwordEncoder, jwtService, yandexOAuthService);

        lenient().when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
        lenient().when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        lenient().when(jwtService.getAccessTokenExpiration()).thenReturn(900_000L);
        lenient().when(jwtService.getRefreshTokenExpiration()).thenReturn(604_800_000L);
    }



    @Test
    @DisplayName("UT-B-05: register отклоняет занятый username с IllegalArgumentException")
    void register_rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("john")).thenReturn(Mono.just(true));

        var request = new RegisterRequest("john", "john@example.com", "password1");

        StepVerifier.create(authService.register(request))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException
                        && t.getMessage().equals("Username already taken"))
                .verify();
    }

    @Test
    @DisplayName("UT-B-06: register отклоняет занятый email с IllegalArgumentException")
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByUsername("john")).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail("john@example.com")).thenReturn(Mono.just(true));

        var request = new RegisterRequest("john", "john@example.com", "password1");

        StepVerifier.create(authService.register(request))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException
                        && t.getMessage().equals("Email already registered"))
                .verify();
    }

    @Test
    @DisplayName("UT-B-07: register happy-path сохраняет пользователя и выдаёт токены")
    void register_happyPathPersistsUserAndIssuesTokens() {
        when(userRepository.existsByUsername("john")).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail("john@example.com")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("password1")).thenReturn("hash");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setId(1L);
                    return Mono.just(u);
                });
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, RefreshToken.class)));

        var request = new RegisterRequest("john", "john@example.com", "password1");

        StepVerifier.create(authService.register(request))
                .assertNext(token -> {
                    assertThat(token.accessToken()).isEqualTo("access-token");
                    assertThat(token.refreshToken()).isEqualTo("refresh-token");
                    assertThat(token.tokenType()).isEqualTo("Bearer");
                    assertThat(token.expiresIn()).isEqualTo(900_000L);
                })
                .verifyComplete();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("john");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getPassword()).isEqualTo("hash");
    }



    @Test
    @DisplayName("UT-B-08: login кидает BadCredentials, если пользователь не найден")
    void login_userNotFound_throwsBadCredentials() {
        when(userRepository.findByUsername("john")).thenReturn(Mono.empty());

        StepVerifier.create(authService.login(new LoginRequest("john", "pwd")))
                .expectErrorMatches(t -> t instanceof BadCredentialsException
                        && t.getMessage().equals("Invalid credentials"))
                .verify();
    }

    @Test
    @DisplayName("UT-B-09: login кидает BadCredentials при неверном пароле")
    void login_wrongPassword_throwsBadCredentials() {
        User user = User.builder().id(1L).username("john").password("hash")
                .enabled(true).build();
        when(userRepository.findByUsername("john")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        StepVerifier.create(authService.login(new LoginRequest("john", "wrong")))
                .expectErrorMatches(t -> t instanceof BadCredentialsException
                        && t.getMessage().equals("Invalid credentials"))
                .verify();
    }

    @Test
    @DisplayName("UT-B-10: login кидает BadCredentials, если аккаунт disabled")
    void login_disabledAccount_throwsBadCredentials() {
        User user = User.builder().id(1L).username("john").password("hash")
                .enabled(false).build();
        when(userRepository.findByUsername("john")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(true);

        StepVerifier.create(authService.login(new LoginRequest("john", "pwd")))
                .expectErrorMatches(t -> t instanceof BadCredentialsException
                        && t.getMessage().equals("Account disabled"))
                .verify();
    }

    @Test
    @DisplayName("UT-B-11: login happy-path возвращает токены")
    void login_happyPath() {
        User user = User.builder().id(1L).username("john").password("hash")
                .enabled(true).role("ROLE_USER").build();
        when(userRepository.findByUsername("john")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, RefreshToken.class)));

        StepVerifier.create(authService.login(new LoginRequest("john", "pwd")))
                .assertNext(token -> {
                    assertThat(token.accessToken()).isEqualTo("access-token");
                    assertThat(token.refreshToken()).isEqualTo("refresh-token");
                })
                .verifyComplete();
    }



    @Test
    @DisplayName("UT-B-12: refresh выдаёт BadCredentials, если токен не найден")
    void refresh_tokenNotFound() {
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Mono.empty());

        StepVerifier.create(authService.refresh("rt"))
                .expectErrorMatches(t -> t instanceof BadCredentialsException
                        && t.getMessage().equals("Refresh token not found"))
                .verify();
    }

    @Test
    @DisplayName("UT-B-13: refresh выдаёт BadCredentials, если токен revoked")
    void refresh_tokenRevoked() {
        RefreshToken rt = RefreshToken.builder()
                .id(1L).token("rt").userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true).build();
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Mono.just(rt));

        StepVerifier.create(authService.refresh("rt"))
                .expectErrorMatches(t -> t instanceof BadCredentialsException
                        && t.getMessage().equals("Refresh token revoked"))
                .verify();
    }

    @Test
    @DisplayName("UT-B-14: refresh выдаёт BadCredentials, если токен expired")
    void refresh_tokenExpired() {
        RefreshToken rt = RefreshToken.builder()
                .id(1L).token("rt").userId(1L)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .revoked(false).build();
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Mono.just(rt));

        StepVerifier.create(authService.refresh("rt"))
                .expectErrorMatches(t -> t instanceof BadCredentialsException
                        && t.getMessage().equals("Refresh token expired"))
                .verify();
    }

    @Test
    @DisplayName("UT-B-15: refresh happy-path — старый токен ротируется, выдан новый")
    void refresh_happyPath_rotatesToken() {
        RefreshToken rt = RefreshToken.builder()
                .id(1L).token("rt").userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false).build();
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Mono.just(rt));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, RefreshToken.class)));

        User user = User.builder().id(1L).username("john").role("ROLE_USER").build();
        when(userRepository.findById(1L)).thenReturn(Mono.just(user));

        StepVerifier.create(authService.refresh("rt"))
                .assertNext(token -> assertThat(token.accessToken()).isNotBlank())
                .verifyComplete();


        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        boolean rotated = captor.getAllValues().stream()
                .anyMatch(t -> "rt".equals(t.getToken()) && t.isRevoked());
        assertThat(rotated).as("revoked-флаг должен быть выставлен у старого токена").isTrue();
    }

    @Test
    @DisplayName("UT-B-15.1: при отсутствии пользователя save не вызывается")
    void register_doesNotSaveWhenUsernameTaken() {
        when(userRepository.existsByUsername("dup")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(
                        new RegisterRequest("dup", "d@e.com", "password1")))
                .expectError()
                .verify();

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }



    @Test
    @DisplayName("UT-B-16: loginWithYandex привязывает yandexId к существующему локальному "
            + "пользователю с тем же e-mail (без создания дубликата)")
    void loginWithYandex_linksExistingLocalUserByEmail() {
        var info = new YandexUserInfo("yid-1", "denis", "denis@yandex.ru",
                "real", "display");

        when(yandexOAuthService.exchangeCodeForToken("code", "uri"))
                .thenReturn(Mono.just(
                        new YandexTokenResponse("AT", 3600L, "bearer", null, null)));
        when(yandexOAuthService.fetchUserInfo("AT")).thenReturn(Mono.just(info));


        when(userRepository.findByYandexId("yid-1")).thenReturn(Mono.empty());

        User existing = User.builder().id(42L).username("denis")
                .email("denis@yandex.ru").password("hash")
                .role("ROLE_USER").enabled(true).provider("LOCAL").build();
        when(userRepository.findByEmail("denis@yandex.ru")).thenReturn(Mono.just(existing));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, User.class)));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, RefreshToken.class)));

        StepVerifier.create(authService.loginWithYandex("code", "uri"))
                .assertNext(t -> assertThat(t.accessToken()).isEqualTo("access-token"))
                .verifyComplete();


        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getYandexId()).isEqualTo("yid-1");
        assertThat(saved.getEmail()).isEqualTo("denis@yandex.ru");
    }

    @Test
    @DisplayName("UT-B-17: loginWithYandex переживает DuplicateKeyException (гонка) — "
            + "перечитывает по yandexId и логинит существующего пользователя")
    void loginWithYandex_recoversFromDuplicateKey_byRereadingByYandexId() {
        var info = new YandexUserInfo("yid-2", "u2", null, "real", "display");

        when(yandexOAuthService.exchangeCodeForToken("code", null))
                .thenReturn(Mono.just(
                        new YandexTokenResponse("AT", 3600L, "bearer", null, null)));
        when(yandexOAuthService.fetchUserInfo("AT")).thenReturn(Mono.just(info));

        User raceWinner = User.builder().id(7L).username("u2")
                .email("yid-2@yandex.local").role("ROLE_USER")
                .enabled(true).provider("YANDEX").yandexId("yid-2").build();



        when(userRepository.findByYandexId("yid-2"))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(raceWinner));

        when(userRepository.existsByUsername("u2")).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail("yid-2@yandex.local"))
                .thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.error(new DuplicateKeyException(
                        "duplicate key value violates unique constraint")));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, RefreshToken.class)));

        StepVerifier.create(authService.loginWithYandex("code", null))
                .assertNext(t -> assertThat(t.accessToken()).isEqualTo("access-token"))
                .verifyComplete();
    }
}
