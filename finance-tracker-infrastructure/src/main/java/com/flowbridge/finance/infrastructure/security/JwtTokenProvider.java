package com.flowbridge.finance.infrastructure.security;

import com.flowbridge.finance.application.port.in.TokenPair;
import com.flowbridge.finance.application.port.out.TokenProvider;
import com.flowbridge.finance.application.port.out.UserRepository;
import com.flowbridge.finance.domain.exception.AuthenticationException;
import com.flowbridge.finance.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider implements TokenProvider {

    private final UserRepository userRepository;
    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;
    
    // In-memory revocation storage (blacklist)
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    public JwtTokenProvider(
            UserRepository userRepository,
            @Value("${app.security.jwt.secret:default-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256-compliance}") String secret,
            @Value("${app.security.jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
            @Value("${app.security.jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays) {
        this.userRepository = userRepository;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Override
    public TokenPair generateTokenPair(User user) {
        String accessToken = generateToken(user, accessTokenExpirationMinutes, ChronoUnit.MINUTES);
        String refreshToken = generateToken(user, refreshTokenExpirationDays * 24 * 60, ChronoUnit.MINUTES);
        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public TokenPair refreshTokens(String refreshToken) {
        if (isTokenRevoked(refreshToken)) {
            throw new AuthenticationException("Refresh token has been revoked");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String userIdStr = claims.getSubject();
            UUID userId = UUID.fromString(userIdStr);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Revoke the old refresh token (Refresh Token Rotation)
            revokeToken(refreshToken);

            // Generate new token pair
            return generateTokenPair(user);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthenticationException("Invalid refresh token", e);
        }
    }

    @Override
    public void revokeToken(String token) {
        if (token != null) {
            revokedTokens.add(token);
        }
    }

    @Override
    public boolean isTokenRevoked(String token) {
        return token != null && revokedTokens.contains(token);
    }

    public boolean validateToken(String token) {
        if (isTokenRevoked(token)) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String generateToken(User user, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant expiry = now.plus(amount, unit);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }
}
