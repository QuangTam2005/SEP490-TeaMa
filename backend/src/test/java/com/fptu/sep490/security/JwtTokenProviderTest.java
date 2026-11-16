package com.fptu.sep490.security;

import com.fptu.sep490.common.exception.AppException;
import com.fptu.sep490.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    @Test
    void plainUtf8AlphanumericSecretWorksWithoutWeakKeyException() {
        // Exactly 32 alphanumeric chars (valid Base64 characters that would shrink to 24 bytes if decoded as Base64)
        JwtProperties props = new JwtProperties();
        props.setSecret("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456");
        props.setExpirationMs(60_000L);

        JwtTokenProvider provider = new JwtTokenProvider(props);
        String token = provider.generateToken(7L, "student@fpt.edu.vn");

        Claims claims = provider.parseAndValidateClaims(token);
        assertEquals(7L, provider.extractUserId(claims));
        assertEquals("student@fpt.edu.vn", claims.get("email", String.class));
    }

    @Test
    void explicitBase64PrefixedSecretDecodesAndValidates() {
        String encoded = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        JwtProperties props = new JwtProperties();
        props.setSecret("base64:" + encoded);
        props.setExpirationMs(60_000L);

        JwtTokenProvider provider = new JwtTokenProvider(props);
        String token = provider.generateToken(99L, "leader@fpt.edu.vn");

        Claims claims = provider.parseAndValidateClaims(token);
        assertNotNull(claims);
        assertEquals(99L, provider.extractUserId(claims));
    }

    @Test
    void expiredTokenThrowsTokenExpiredAppException() {
        JwtProperties props = new JwtProperties();
        props.setSecret("sep490_teama_unit_test_secret_key_32_bytes");
        props.setExpirationMs(-1_000L);

        JwtTokenProvider provider = new JwtTokenProvider(props);
        String expiredToken = provider.generateToken(1L, "expired@fpt.edu.vn");

        AppException ex = assertThrows(AppException.class, () -> provider.parseAndValidateClaims(expiredToken));
        assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
    }

    @Test
    void tamperedTokenThrowsUnauthorizedAppException() {
        JwtProperties props = new JwtProperties();
        props.setSecret("sep490_teama_unit_test_secret_key_32_bytes");
        props.setExpirationMs(60_000L);

        JwtTokenProvider provider = new JwtTokenProvider(props);
        String token = provider.generateToken(1L, "user@fpt.edu.vn");
        String tampered = token + "x";

        AppException ex = assertThrows(AppException.class, () -> provider.parseAndValidateClaims(tampered));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void shortSecretThrowsIllegalStateException() {
        JwtProperties props = new JwtProperties();
        props.setSecret("too-short");
        JwtTokenProvider provider = new JwtTokenProvider(props);

        assertThrows(IllegalStateException.class, () -> provider.generateToken(1L, "a@b.com"));
    }
}
