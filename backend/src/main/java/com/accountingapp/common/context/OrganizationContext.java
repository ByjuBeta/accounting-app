package com.accountingapp.common.context;

import java.util.UUID;

/**
 * The organization (tenant) the current request is operating against.
 * Populated per-request by {@link OrganizationContextFilter}. Until the JWT
 * auth chain lands (see the auth/settings block), the filter trusts an
 * {@code X-Organization-Id} header; afterward it will be derived from the
 * authenticated user's active organization membership instead — callers of
 * {@link #getRequired()} don't need to change either way.
 */
public final class OrganizationContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private OrganizationContext() {
    }

    public static void set(UUID organizationId) {
        CURRENT.set(organizationId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static UUID getRequired() {
        UUID organizationId = CURRENT.get();
        if (organizationId == null) {
            throw new IllegalStateException(
                    "No organization context on this request — missing X-Organization-Id header");
        }
        return organizationId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
