package com.asc.studycompanion.config;

import com.asc.studycompanion.security.JwtAuthFilter;
import com.asc.studycompanion.security.RestAccessDeniedHandler;
import com.asc.studycompanion.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityCorsTest {

    @Test
    void corsConfigurationSplitsCommaSeparatedOriginsAndExposesContentDisposition() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthFilter.class),
                mock(RestAuthenticationEntryPoint.class),
                mock(RestAccessDeniedHandler.class)
        );
        ReflectionTestUtils.setField(
                securityConfig,
                "allowedOriginsRaw",
                "http://localhost:3000, https://teama.fptu.edu.vn , "
        );

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(config);
        assertEquals(
                List.of("http://localhost:3000", "https://teama.fptu.edu.vn"),
                config.getAllowedOrigins()
        );
        assertTrue(config.getExposedHeaders().contains("Content-Disposition"));
        assertTrue(config.getAllowedHeaders().contains("Idempotency-Key"));
    }
}
