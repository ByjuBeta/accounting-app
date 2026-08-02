package com.accountingapp.banking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    Optional<BankTransaction> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByAccountIdAndExternalId(UUID accountId, String externalId);

    List<BankTransaction> findByAccountIdAndStatusOrderByTransactionDateAsc(UUID accountId, BankTransactionStatus status);

    List<BankTransaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId);

    @Query("""
            select bt from BankTransaction bt
            where bt.account.id = :accountId
              and bt.status = com.accountingapp.banking.BankTransactionStatus.MATCHED
              and bt.matchedJournalEntryLine.reconciled = false
              and bt.transactionDate <= :asOfDate
            """)
    List<BankTransaction> findClearedUnreconciled(@Param("accountId") UUID accountId, @Param("asOfDate") LocalDate asOfDate);
}
