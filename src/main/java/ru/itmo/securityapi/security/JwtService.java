package ru.itmo.securityapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class JwtService {

    private final SecretKey signingKey;
    private final Duration lifetime;
    private final Clock clock;

    public JwtService(
            @Value("${app.jwt.secret-base64}") String secretBase64,
            @Value("${app.jwt.lifetime-minutes:15}") long lifetimeMinutes) {
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("APP_JWT_SECRET_BASE64 must contain at least 32 random bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.lifetime = Duration.ofMinutes(lifetimeMinutes);
        this.clock = Clock.systemUTC();
    }

    public String issueToken(String username) {
        Instant issuedAt = clock.instant();
        return Jwts.builder()
                .subject(username)
                .issuer("secure-api-lab1")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(lifetime)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, String expectedUsername) {
        Claims claims = parseClaims(token);
        return expectedUsername.equals(claims.getSubject())
                && claims.getExpiration().after(Date.from(clock.instant()));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer("secure-api-lab1")
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
