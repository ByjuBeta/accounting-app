package com.accountingapp.user;

import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.AuthContext;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.common.exception.ConflictException;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.organization.Organization;
import com.accountingapp.organization.OrganizationRepository;
import com.accountingapp.user.dto.AddMemberRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationMemberService {

    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<OrganizationMember> list() {
        return organizationMemberRepository.findByOrganizationIdAndActiveTrueOrderByCreatedAtAsc(
                OrganizationContext.getRequired());
    }

    /**
     * Adds an existing user as a member. There's no email-invite flow in this system — the user must
     * already have an account (asked to register separately) before they can be added.
     */
    @Transactional
    public OrganizationMember add(AddMemberRequest request) {
        UUID organizationId = OrganizationContext.getRequired();
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found for " + request.email() + " — they need to register first."));
        if (organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, user.getId()).isPresent()) {
            throw new ConflictException("ALREADY_A_MEMBER", "This user is already a member of this organization.");
        }
        Organization organization = organizationRepository.getReferenceById(organizationId);
        OrganizationMember member = OrganizationMember.builder()
                .user(user)
                .role(request.role())
                .active(true)
                .build();
        member.setOrganization(organization);
        OrganizationMember saved = organizationMemberRepository.save(member);
        auditLogService.record("ADD_MEMBER", "OrganizationMember", saved.getId(),
                "Added " + user.getEmail() + " as " + request.role(), null);
        return saved;
    }

    @Transactional
    public OrganizationMember updateRole(UUID memberId, Role newRole) {
        OrganizationMember member = getOrThrow(memberId);
        guardLastAdmin(member, newRole);
        member.setRole(newRole);
        OrganizationMember saved = organizationMemberRepository.save(member);
        auditLogService.record("UPDATE_MEMBER_ROLE", "OrganizationMember", saved.getId(),
                "Changed " + member.getUser().getEmail() + "'s role to " + newRole, null);
        return saved;
    }

    @Transactional
    public void remove(UUID memberId) {
        OrganizationMember member = getOrThrow(memberId);
        if (member.getUser().getId().equals(AuthContext.requireUserId())) {
            throw new BusinessRuleException("CANNOT_REMOVE_SELF", "You cannot remove yourself from the organization.");
        }
        guardLastAdmin(member, null);
        member.setActive(false);
        organizationMemberRepository.save(member);
        auditLogService.record("REMOVE_MEMBER", "OrganizationMember", member.getId(),
                "Removed " + member.getUser().getEmail(), null);
    }

    /** Refuses to demote or remove the organization's last active admin. */
    private void guardLastAdmin(OrganizationMember member, Role newRole) {
        boolean losingAdmin = member.getRole() == Role.ADMIN && newRole != Role.ADMIN;
        if (!losingAdmin) {
            return;
        }
        long adminCount = organizationMemberRepository.countByOrganizationIdAndRoleAndActiveTrue(
                OrganizationContext.getRequired(), Role.ADMIN);
        if (adminCount <= 1) {
            throw new BusinessRuleException("LAST_ADMIN", "This organization must have at least one admin.");
        }
    }

    private OrganizationMember getOrThrow(UUID memberId) {
        UUID organizationId = OrganizationContext.getRequired();
        OrganizationMember member = organizationMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationMember", memberId));
        if (!member.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("OrganizationMember", memberId);
        }
        return member;
    }
}
