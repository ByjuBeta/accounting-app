package com.accountingapp.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByUserIdAndActiveTrue(UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    List<OrganizationMember> findByOrganizationIdAndActiveTrueOrderByCreatedAtAsc(UUID organizationId);

    long countByOrganizationIdAndRoleAndActiveTrue(UUID organizationId, Role role);
}
