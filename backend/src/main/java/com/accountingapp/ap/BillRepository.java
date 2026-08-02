package com.accountingapp.ap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    Optional<Bill> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Bill> findByOrganizationIdOrderByDueDateAscBillNumberAsc(UUID organizationId, Pageable pageable);

    List<Bill> findByOrganizationIdAndStatusIn(UUID organizationId, List<BillStatus> statuses);
}
