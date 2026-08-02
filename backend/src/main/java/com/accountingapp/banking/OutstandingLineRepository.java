package com.accountingapp.banking;

import com.accountingapp.journal.JournalEntryLine;
import com.accountingapp.journal.TransactionStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Finds GL lines on a bank account that have no matched bank-feed
 * transaction yet — i.e. outstanding checks/deposits for a reconciliation
 * worksheet. Lives in {@code banking} (not alongside {@link JournalEntryLine})
 * so the journal module doesn't need to know banking exists.
 */
public interface OutstandingLineRepository extends JpaRepository<JournalEntryLine, UUID> {

    @Query("""
            select l from JournalEntryLine l
            where l.account.id = :accountId
              and l.reconciled = false
              and l.journalEntry.status in :statuses
              and l.journalEntry.entryDate <= :asOfDate
              and not exists (
                  select 1 from BankTransaction bt
                  where bt.matchedJournalEntryLine = l
              )
            order by l.journalEntry.entryDate asc
            """)
    List<JournalEntryLine> findOutstandingLines(
            @Param("accountId") UUID accountId,
            @Param("asOfDate") LocalDate asOfDate,
            @Param("statuses") Collection<TransactionStatus> statuses);
}
