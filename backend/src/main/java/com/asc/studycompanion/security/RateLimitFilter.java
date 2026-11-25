package com.asc.studycompanion.security;

import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Lightweight in-memory fixed-window rate limiter protecting auth and AI submission endpoints
 * from brute-force or excessive token consumption (returns 429 SYS_429).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final Pattern SUBMISSION_POST_PATTERN = Pattern.compile(
            "^/api/student/(tasks/[^/]+/submissions|submissions/[^/]+/retry-evaluation)$"
    );

    private final ObjectMapper objectMapper;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled = true;

    @Value("${app.rate-limit.auth-per-minute:15}")
    private int authLimitPerMinute = 15;

    @Value("${app.rate-limit.submit-per-minute:20}")
    private int submitLimitPerMinute = 20;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void configureLimits(boolean enabled, int authLimitPerMinute, int submitLimitPerMinute) {
        this.enabled = enabled;
        this.authLimitPerMinute = authLimitPerMinute;
        this.submitLimitPerMinute = submitLimitPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || !HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        LimitPolicy policy = resolvePolicy(path);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String actorKey = policy.bucketPrefix() + ":" + resolveActorIdentifier(request, policy.preferAuthenticatedUser());
        long nowMs = clock.millis();

        if (counters.size() > MAX_TRACKED_KEYS) {
            counters.entrySet().removeIf(entry -> (nowMs - entry.getValue().windowStartMs) >= WINDOW_MS);
        }

        WindowCounter updated = counters.compute(actorKey, (k, existing) -> {
            if (existing == null || (nowMs - existing.windowStartMs) >= WINDOW_MS) {
                return new WindowCounter(nowMs, 1);
            }
            return new WindowCounter(existing.windowStartMs, existing.count + 1);
        });

        if (updated.count > policy.maxRequests()) {
            long retryAfterSeconds = Math.max(1L, ((updated.windowStartMs + WINDOW_MS) - nowMs + 999L) / 1000L);
            response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus().value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.error(ErrorCode.RATE_LIMIT_EXCEEDED)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private LimitPolicy resolvePolicy(String path) {
        if ("/api/auth/login".equals(path) || "/api/auth/register".equals(path)) {
            return new LimitPolicy("auth", authLimitPerMinute, false);
        }
        if (path != null && SUBMISSION_POST_PATTERN.matcher(path).matches()) {
            return new LimitPolicy("submit", submitLimitPerMinute, true);
        }
        return null;
    }

    private String resolveActorIdentifier(HttpServletRequest request, boolean preferAuthenticatedUser) {
        if (preferAuthenticatedUser) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal && principal.getId() != null) {
                return "user-" + principal.getId();
            }
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return "ip-" + first;
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return "ip-" + (remoteAddr != null ? remoteAddr : "unknown");
    }

    private record LimitPolicy(String bucketPrefix, int maxRequests, boolean preferAuthenticatedUser) {
    }

    private record WindowCounter(long windowStartMs, int count) {
    }
}
