package com.kaankaplan.userService.core.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtProviderServiceImpl implements JwtProviderService {

    private final SecretKey secretKey;
    private final long expirationDays;

    public JwtProviderServiceImpl(
            @Value("${jwt.secret.key}") String key,
            @Value("${jwt.expiration-days:14}") long expirationDays
    ) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("JWT secret key is required. Configure JWT_SECRET_KEY from AWS Secrets Manager or local environment.");
        }
        if (key.length() < 32) {
            throw new IllegalStateException("JWT secret key must be at least 32 characters long.");
        }
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        this.expirationDays = expirationDays;
    }

    @Override
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("authorities", authentication.getAuthorities())
                .subject(authentication.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationDays, ChronoUnit.DAYS)))
                .signWith(secretKey)
                .compact();
    }
}
