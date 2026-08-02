package com.accountingapp.banking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    Optional<Reconciliation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Reconciliation> findByAccountIdOrderByStatementDateDesc(UUID accountId);

    Optional<Reconciliation> findFirstByAccountIdAndStatusOrderByStatementDateDesc(
            UUID accountId, ReconciliationStatus status);

    boolean existsByAccountIdAndStatus(UUID accountId, ReconciliationStatus status);
}
