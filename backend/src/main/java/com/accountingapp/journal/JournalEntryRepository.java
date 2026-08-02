package com.accountingapp.journal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<JournalEntry> findByOrganizationIdOrderByEntryDateDescEntryNumberDesc(
            UUID organizationId, Pageable pageable);

    @Query("select count(e) from JournalEntry e where e.organization.id = :organizationId")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);
}
