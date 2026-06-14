package com.example.Back.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
/**
 * Generates and validates access/refresh JSON Web Tokens. Uses HMAC-SHA256 signing, embeds the username and userId claims and enforces a configurable expiration.
 */

@Service
public class JwtService {

    private final SecretKey secretKey;
    @Getter
    private final long accessTokenExpiration;
    @Getter
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Generates a short-lived access token for the given subject with a
     * {@code role} claim.
     *
     * @param username token subject
     * @param role     authority granted to the token holder
     * @return signed compact JWT
     */
    public String generateAccessToken(String username, String role) {
        return buildToken(Map.of("role", role), username, accessTokenExpiration);
    }

    /**
     * Generates a long-lived refresh token for the given subject. The token
     * carries no extra claims; it is later swapped for a new pair via
     * {@link com.example.Back.service.AuthService#refresh(String)}.
     *
     * @param username token subject
     * @return signed compact JWT
     */
    public String generateRefreshToken(String username) {
        return buildToken(Map.of(), username, refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the {@code sub} claim (username) from a previously generated
     * JWT. Throws if the signature does not match the service key.
     *
     * @param token compact JWT
     * @return username embedded in the {@code sub} claim
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Validates the signature and the expiration of {@code token}. Any error
     * (malformed, bad signature, expired) results in {@code false}; no
     * exception is thrown.
     *
     * @param token compact JWT
     * @return {@code true} if the token is currently valid
     */
    public boolean isTokenValid(String token) {
        try {
            return !extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
