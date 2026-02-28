package com.scube.society_app_backend.service;

import com.scube.society_app_backend.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "tt";
    private static final String TOKEN_TYPE_ACCESS = "access";

    private final SecretKey key;
    private final int accessTtlMin;
    private final int refreshTtlDays;

    public JwtService(AuthProperties authProperties) {
        AuthProperties.Jwt jwt = authProperties.getJwt();
        String secret = jwt.getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMin = jwt.getAccessTtlMin();
        this.refreshTtlDays = jwt.getRefreshTtlDays();
    }

    /** Issue a short-lived access token for the user. */
    public String issueAccessToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(TimeUnit.MINUTES.toSeconds(accessTtlMin));
        String sub = userId.toString();
        return Jwts.builder()
                .subject(sub)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    /** Access token TTL in seconds (for expires_in in response). */
    public long getAccessTtlSeconds() {
        return TimeUnit.MINUTES.toSeconds(accessTtlMin);
    }

    /** Create a new refresh token: returns raw token, its hash, and expiry. */
    public RefreshTokenData createRefreshToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        String raw = bytesToHex(bytes);
        String hash = hashRefreshToken(raw);
        Instant expiresAt = Instant.now().plus(refreshTtlDays, java.time.temporal.ChronoUnit.DAYS);
        return new RefreshTokenData(raw, hash, expiresAt);
    }

    /** Validate access token and return user id; throws if invalid. */
    public UUID validateAccessToken(String token) {
        Claims payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TOKEN_TYPE_ACCESS.equals(payload.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new IllegalArgumentException("invalid access token");
        }
        String sub = payload.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("invalid access token");
        }
        return UUID.fromString(sub);
    }

    /** SHA-256 hash of refresh token for DB storage. */
    public static String hashRefreshToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public record RefreshTokenData(String rawToken, String tokenHash, Instant expiresAt) {}
}
