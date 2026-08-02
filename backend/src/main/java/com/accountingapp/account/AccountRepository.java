package com.accountingapp.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByOrganizationIdOrderByCodeAsc(UUID organizationId);

    List<Account> findByOrganizationIdAndStatusOrderByCodeAsc(UUID organizationId, AccountStatus status);

    Optional<Account> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Account> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByParentId(UUID parentId);
}
