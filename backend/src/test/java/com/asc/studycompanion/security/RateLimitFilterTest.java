package com.asc.studycompanion.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    @Test
    void enforcesPerMinuteLimitReturns429Sys429AndResetsNextMinute() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RateLimitFilter filter = new RateLimitFilter(objectMapper);
        filter.configureLimits(true, 2, 2);

        AtomicLong currentMs = new AtomicLong(Instant.parse("2026-11-30T10:00:00Z").toEpochMilli());
        filter.setClock(new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(currentMs.get());
            }
        });

        for (int attempt = 1; attempt <= 2; attempt++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("10.0.0.42");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }

        // 3rd request inside same 60s window triggers 429 SYS_429
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setRemoteAddr("10.0.0.42");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilter(blockedReq, blockedRes, new MockFilterChain());
        assertEquals(429, blockedRes.getStatus());
        assertNotNull(blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("\"errorCode\":\"SYS_429\""));

        // Different IP is unaffected
        MockHttpServletRequest otherIpReq = new MockHttpServletRequest("POST", "/api/auth/login");
        otherIpReq.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse otherIpRes = new MockHttpServletResponse();
        filter.doFilter(otherIpReq, otherIpRes, new MockFilterChain());
        assertEquals(200, otherIpRes.getStatus());

        // Advance clock by 61 seconds -> bucket resets cleanly
        currentMs.addAndGet(61_000L);
        MockHttpServletRequest resetReq = new MockHttpServletRequest("POST", "/api/auth/login");
        resetReq.setRemoteAddr("10.0.0.42");
        MockHttpServletResponse resetRes = new MockHttpServletResponse();
        filter.doFilter(resetReq, resetRes, new MockFilterChain());
        assertEquals(200, resetRes.getStatus());
    }
}
