package com.accountingapp.ar;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Invoice> findByOrganizationIdOrderByInvoiceDateDescInvoiceNumberDesc(UUID organizationId, Pageable pageable);

    List<Invoice> findByOrganizationIdAndStatusIn(UUID organizationId, List<InvoiceStatus> statuses);

    List<Invoice> findByCustomerIdAndStatusIn(UUID customerId, List<InvoiceStatus> statuses);
}
