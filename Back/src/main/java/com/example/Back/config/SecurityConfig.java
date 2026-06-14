package com.example.Back.config;


import com.example.Back.security.JwtAuthenticationFilter;
import com.example.Back.security.ReactiveUserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
/**
 * Spring Security (WebFlux) configuration: JWT authentication filter, CORS policy, stateless session, and explicit 401/403 entry points.
 */

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper mapper) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            exchange.getResponse().getHeaders()
                                    .setContentType(MediaType.APPLICATION_JSON);
                            byte[] body = writeJson(mapper, Map.of(
                                    "error", "Unauthorized",
                                    "message", e.getMessage()));
                            return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse()
                                            .bufferFactory().wrap(body)));
                        })
                        .accessDeniedHandler((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            exchange.getResponse().getHeaders()
                                    .setContentType(MediaType.APPLICATION_JSON);
                            byte[] body = writeJson(mapper, Map.of(
                                    "error", "Forbidden",
                                    "message", e.getMessage()));
                            return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse()
                                            .bufferFactory().wrap(body)));
                        })
                )
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET,  "/api/v1/templates/public").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsServiceImpl userDetailsService) {
        var manager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder());
        return manager;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private byte[] writeJson(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (Exception e) {
            return "{}".getBytes();
        }
    }
}