package com.example.Back.controller;

import com.example.Back.dto.*;
import com.example.Back.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
/**
 * REST endpoints for authentication: register, login, refresh, logout, current user and Yandex callback.
 */

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public Mono<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public Mono<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {

        return authService.logout(null);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Mono<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return authService.getCurrentUser(userDetails.getUsername());
    }


    @PostMapping("/yandex/callback")
    public Mono<TokenResponse> yandexCallback(
            @Valid @RequestBody YandexCallbackRequest request) {
        return authService.loginWithYandex(request.code(), request.redirectUri());
    }
}
