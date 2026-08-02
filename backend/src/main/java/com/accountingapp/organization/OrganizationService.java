package com.accountingapp.organization;

import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.organization.dto.CreateOrganizationRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AuditLogService auditLogService;

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

    /**
     * Lists every organization with no scoping — acceptable only until the
     * auth block wires real login, at which point this must scope to the
     * caller's {@link com.accountingapp.user.OrganizationMember} rows.
     */
    @Transactional(readOnly = true)
    public List<Organization> listAll() {
        return organizationRepository.findAll();
    }
}
