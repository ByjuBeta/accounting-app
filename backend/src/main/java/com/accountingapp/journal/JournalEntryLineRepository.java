package com.accountingapp.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, UUID> {

    interface AccountTotals {
        BigDecimal getTotalDebit();

        BigDecimal getTotalCredit();
    }

    interface AccountTotalsByAccount extends AccountTotals {
        UUID getAccountId();
    }

    List<JournalEntryLine> findByJournalEntryIdOrderByLineNumberAsc(UUID journalEntryId);

    boolean existsByAccountId(UUID accountId);

    @Query("""
            select l from JournalEntryLine l
            where l.account.id = :accountId
              and l.reconciled = false
              and l.journalEntry.status in :statuses
              and l.journalEntry.entryDate between :fromDate and :toDate
              and (l.debitAmount - l.creditAmount) = :signedAmount
            order by l.journalEntry.entryDate asc
            """)
    List<JournalEntryLine> findUnreconciledCandidates(
            @Param("accountId") UUID accountId,
            @Param("signedAmount") java.math.BigDecimal signedAmount,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate,
            @Param("statuses") Collection<TransactionStatus> statuses);

    @Query("""
            select coalesce(sum(l.debitAmount), 0) as totalDebit, coalesce(sum(l.creditAmount), 0) as totalCredit
            from JournalEntryLine l
            where l.account.id = :accountId
              and l.journalEntry.organization.id = :organizationId
              and l.journalEntry.status in :statuses
              and l.journalEntry.entryDate <= :asOfDate
            """)
    AccountTotals sumForAccountAsOf(
            @Param("organizationId") UUID organizationId,
            @Param("accountId") UUID accountId,
            @Param("asOfDate") LocalDate asOfDate,
            @Param("statuses") Collection<TransactionStatus> statuses);

    @Query("""
            select l.account.id as accountId,
                   coalesce(sum(l.debitAmount), 0) as totalDebit,
                   coalesce(sum(l.creditAmount), 0) as totalCredit
            from JournalEntryLine l
            where l.journalEntry.organization.id = :organizationId
              and l.journalEntry.status in :statuses
              and l.journalEntry.entryDate <= :asOfDate
            group by l.account.id
            """)
    List<AccountTotalsByAccount> sumForAllAccountsAsOf(
            @Param("organizationId") UUID organizationId,
            @Param("asOfDate") LocalDate asOfDate,
            @Param("statuses") Collection<TransactionStatus> statuses);

    @Query("""
            select l.account.id as accountId,
                   coalesce(sum(l.debitAmount), 0) as totalDebit,
                   coalesce(sum(l.creditAmount), 0) as totalCredit
            from JournalEntryLine l
            where l.journalEntry.organization.id = :organizationId
              and l.journalEntry.status in :statuses
              and l.journalEntry.entryDate between :fromDate and :toDate
            group by l.account.id
            """)
    List<AccountTotalsByAccount> sumForAllAccountsInRange(
            @Param("organizationId") UUID organizationId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") Collection<TransactionStatus> statuses);

    @Query("""
            select l from JournalEntryLine l
            where l.account.id in :accountIds
              and l.journalEntry.organization.id = :organizationId
              and l.journalEntry.status in :statuses
              and l.journalEntry.entryDate between :fromDate and :toDate
            order by l.journalEntry.entryDate asc, l.lineNumber asc
            """)
    List<JournalEntryLine> findByAccountIdsAndDateRange(
            @Param("organizationId") UUID organizationId,
            @Param("accountIds") Collection<UUID> accountIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") Collection<TransactionStatus> statuses);
}
