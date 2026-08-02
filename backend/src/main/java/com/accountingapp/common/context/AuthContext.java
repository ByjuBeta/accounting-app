package com.accountingapp.common.context;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The authenticated user on the current request. {@link com.accountingapp.auth.JwtAuthenticationFilter}
 * sets the {@link Authentication} name to the user's id (see also
 * {@code JpaAuditingConfig} and {@code AuditLogService}, which rely on the same convention).
 */
public final class AuthContext {

    private AuthContext() {
    }

    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static UUID requireUserId() {
        UUID userId = currentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user on this request");
        }
        return userId;
    }
}
