package com.accountingapp.common.context;

import com.accountingapp.common.exception.ErrorResponse;
import com.accountingapp.user.OrganizationMember;
import com.accountingapp.user.OrganizationMemberRepository;
import com.accountingapp.user.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the {@code X-Organization-Id} header into {@link OrganizationContext}, and — for
 * authenticated requests — verifies the caller is an active member of that organization and applies
 * a coarse read/write gate: {@link Role#VIEWER} and {@link Role#LIMITED_USER} may only make GET
 * requests. This is deliberately a single filter-level check rather than {@code @PreAuthorize} on
 * every existing controller method (15+ controllers); finer-grained checks (e.g. admin-only member
 * management) use {@code @PreAuthorize("@access.isOrgAdmin()")} on the specific endpoints that need
 * them — see {@link com.accountingapp.auth.AccessControl}.
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class OrganizationContextFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Organization-Id";
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final OrganizationMemberRepository organizationMemberRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER_NAME);
        try {
            if (header != null && !header.isBlank()) {
                UUID organizationId;
                try {
                    organizationId = UUID.fromString(header);
                } catch (IllegalArgumentException ex) {
                    writeError(response, request, HttpStatus.BAD_REQUEST, "INVALID_ORGANIZATION_HEADER",
                            "X-Organization-Id must be a valid UUID.");
                    return;
                }

                UUID userId = AuthContext.currentUserId();
                if (userId != null) {
                    OrganizationMember member = organizationMemberRepository
                            .findByOrganizationIdAndUserId(organizationId, userId)
                            .filter(OrganizationMember::isActive)
                            .orElse(null);
                    if (member == null) {
                        writeError(response, request, HttpStatus.FORBIDDEN, "NOT_A_MEMBER",
                                "You do not have access to this organization.");
                        return;
                    }
                    if (MUTATING_METHODS.contains(request.getMethod())
                            && (member.getRole() == Role.VIEWER || member.getRole() == Role.LIMITED_USER)) {
                        writeError(response, request, HttpStatus.FORBIDDEN, "READ_ONLY_ROLE",
                                "Your role in this organization only allows read access.");
                        return;
                    }
                    CurrentMembershipContext.set(member.getRole());
                }
                OrganizationContext.set(organizationId);
            }
            filterChain.doFilter(request, response);
        } finally {
            OrganizationContext.clear();
            CurrentMembershipContext.clear();
        }
    }

    private void writeError(
            HttpServletResponse response, HttpServletRequest request, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        ErrorResponse body = ErrorResponse.of(status.value(), code, message, request.getRequestURI(), null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
