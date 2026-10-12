package com.ansar.secureapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey key;
    private final long ttlSeconds;
    private final String issuer = "spring-secure-api";

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-seconds:900}") long ttlSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Returns claims only if signature, algorithm, issuer and expiry are all valid. */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                    .parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getTtlSeconds() { return ttlSeconds; }
}
