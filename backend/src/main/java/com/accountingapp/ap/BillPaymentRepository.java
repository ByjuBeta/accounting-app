package com.accountingapp.ap;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillPaymentRepository extends JpaRepository<BillPayment, UUID> {

    Optional<BillPayment> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<BillPayment> findByOrganizationIdOrderByPaymentDateDesc(UUID organizationId, Pageable pageable);
}
