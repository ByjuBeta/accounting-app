package com.accountingapp.audit;

import com.accountingapp.common.context.OrganizationContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /** Records an audit entry for the given entity, tagged with the current user/IP/organization. */
    public void record(String action, String entityType, UUID entityId, String summary, Map<String, Object> changes) {
        AuditLog log = AuditLog.builder()
                .organizationId(OrganizationContext.getRequired())
                .userId(currentUserId())
                .ipAddress(currentClientIp())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .summary(summary)
                .changes(changes)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(log);
    }

    private UUID currentUserId() {
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

    private String currentClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
