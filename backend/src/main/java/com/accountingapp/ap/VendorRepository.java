package com.accountingapp.ap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<Vendor> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
