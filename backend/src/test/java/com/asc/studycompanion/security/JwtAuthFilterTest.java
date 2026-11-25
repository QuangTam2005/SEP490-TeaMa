package com.asc.studycompanion.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.common.util.SecurityAuditorHelper;
import com.asc.studycompanion.module.auth.entity.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private CustomUserDetailsService userDetailsService;

    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthFilter jwtAuthFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("sep490_teama_unit_test_secret_key_32_bytes");
        props.setExpirationMs(60_000L);

        jwtTokenProvider = new JwtTokenProvider(props);
        jwtAuthFilter = new JwtAuthFilter(jwtTokenProvider, userDetailsService);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesPrincipalAndPopulatesAuditorUserId() throws Exception {
        String token = jwtTokenProvider.generateToken(42L, "hocsinh@gmail.com");
        UserPrincipal principal = new UserPrincipal(42L, "hocsinh@gmail.com", "hash", "Nguyen Minh Khoi", UserRole.STUDENT);
        when(userDetailsService.loadUserById(42L)).thenReturn(principal);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(42L, SecurityAuditorHelper.getCurrentUserId().orElse(null));
        assertTrue(principal.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority())));
        assertNull(request.getAttribute(JwtAuthFilter.AUTH_ERROR_CODE_ATTR));
    }

    @Test
    void missingUserInDbClearsContextAndSetsUnauthorizedAttrWithoutCrashing() throws Exception {
        String token = jwtTokenProvider.generateToken(999L, "deleted@fpt.edu.vn");
        when(userDetailsService.loadUserById(999L))
                .thenThrow(new UsernameNotFoundException("User not found"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(ErrorCode.UNAUTHORIZED, request.getAttribute(JwtAuthFilter.AUTH_ERROR_CODE_ATTR));

        // Verify RestAuthenticationEntryPoint writes clean 401 ApiResponse JSON
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        entryPoint.commence(request, response, new BadCredentialsException("Unauthorized"));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"AUTH_001\""));
    }

    @Test
    void invalidBearerTokenClearsContextAndSetsUnauthorizedAttr() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(ErrorCode.UNAUTHORIZED, request.getAttribute(JwtAuthFilter.AUTH_ERROR_CODE_ATTR));
    }

    @Test
    void accessDeniedHandlerWrites403ApiResponseJson() throws Exception {
        RestAccessDeniedHandler deniedHandler = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(request, response, new AccessDeniedException("Forbidden"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"AUTH_004\""));
    }
}
