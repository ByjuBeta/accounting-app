package com.accountingapp.ar;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<Customer> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
