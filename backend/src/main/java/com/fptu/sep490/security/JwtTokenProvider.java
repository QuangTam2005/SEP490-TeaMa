package com.fptu.sep490.security;

import com.fptu.sep490.common.exception.AppException;
import com.fptu.sep490.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generates and validates HMAC-SHA256 JWT tokens.
 * Uses UTF-8 secret bytes by default; decodes Base64 only when prefixed with "base64:".
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String BASE64_PREFIX = "base64:";
    private static final int MIN_KEY_BYTES = 32;

    private final JwtProperties jwtProperties;

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAndValidateClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    public Long extractUserId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private SecretKey getSigningKey() {
        String rawSecret = jwtProperties.getSecret();
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }

        byte[] keyBytes;
        if (rawSecret.startsWith(BASE64_PREFIX)) {
            String encoded = rawSecret.substring(BASE64_PREFIX.length()).trim();
            keyBytes = Decoders.BASE64.decode(encoded);
        } else {
            keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes (256 bits)");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
