package com.asc.studycompanion.common.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Safely resolves the authenticated user's numeric ID from Spring SecurityContext.
 * Returns Optional.empty() for anonymous, webhook, or magic-link requests.
 */
public final class SecurityAuditorHelper {

    private SecurityAuditorHelper() {
    }

    public static Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return Optional.of(number.longValue());
        }

        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(name));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
