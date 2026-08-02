package com.accountingapp.organization;

import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.AuthContext;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.organization.dto.CreateOrganizationRequest;
import com.accountingapp.organization.dto.UpdateOrganizationRequest;
import com.accountingapp.user.OrganizationMember;
import com.accountingapp.user.OrganizationMemberRepository;
import com.accountingapp.user.Role;
import com.accountingapp.user.User;
import com.accountingapp.user.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /** Creates the organization and grants the creating user {@link Role#ADMIN} on it. */
    @Transactional
    public Organization create(CreateOrganizationRequest request) {
        Organization organization = Organization.builder()
                .name(request.name())
                .legalName(request.legalName())
                .baseCurrencyCode(request.baseCurrencyCode().toUpperCase())
                .timeZone(request.timeZone())
                .active(true)
                .build();
        Organization saved = organizationRepository.save(organization);

        OrganizationContext.set(saved.getId());
        try {
            User creator = userRepository.getReferenceById(AuthContext.requireUserId());
            OrganizationMember member = OrganizationMember.builder()
                    .user(creator)
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            member.setOrganization(saved);
            organizationMemberRepository.save(member);
            auditLogService.record("CREATE", "Organization", saved.getId(), "Created organization " + saved.getName(), null);
        } finally {
            OrganizationContext.clear();
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Organization getOrThrow(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
    }

    /** Lists only the organizations the current user is an active member of. */
    @Transactional(readOnly = true)
    public List<Organization> listForCurrentUser() {
        return organizationMemberRepository.findByUserIdAndActiveTrue(AuthContext.requireUserId()).stream()
                .map(OrganizationMember::getOrganization)
                .toList();
    }

    @Transactional
    public Organization update(UUID organizationId, UpdateOrganizationRequest request) {
        if (!organizationId.equals(OrganizationContext.getRequired())) {
            throw new AccessDeniedException("Organization id must match the X-Organization-Id header.");
        }
        Organization organization = getOrThrow(organizationId);
        organization.setName(request.name());
        organization.setLegalName(request.legalName());
        organization.setTimeZone(request.timeZone());
        Organization saved = organizationRepository.save(organization);
        auditLogService.record("UPDATE", "Organization", saved.getId(), "Updated organization settings", null);
        return saved;
    }
}
