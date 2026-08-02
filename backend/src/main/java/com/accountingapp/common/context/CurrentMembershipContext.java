package com.accountingapp.common.context;

import com.accountingapp.user.Role;

/**
 * The authenticated user's {@link Role} within the current request's organization
 * (see {@link OrganizationContext}). Populated by {@code OrganizationContextFilter}
 * once it has verified the caller is an active member of that organization.
 */
public final class CurrentMembershipContext {

    private static final ThreadLocal<Role> CURRENT = new ThreadLocal<>();

    private CurrentMembershipContext() {
    }

    public static void set(Role role) {
        CURRENT.set(role);
    }

    public static Role get() {
        return CURRENT.get();
    }

    public static boolean isAdmin() {
        return CURRENT.get() == Role.ADMIN;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
