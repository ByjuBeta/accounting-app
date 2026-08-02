package com.accountingapp.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class JpaAuditingConfig {

    /**
     * Resolves the current user for {@code @CreatedBy}/{@code @LastModifiedBy}.
     * The JWT auth filter (see the security package) sets the authenticated
     * principal's name to the user's id, so unauthenticated/system writes
     * (seed data, scheduled jobs) simply get no auditor.
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            try {
                return Optional.of(UUID.fromString(authentication.getName()));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        };
    }
}
