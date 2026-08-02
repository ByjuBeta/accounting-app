package com.accountingapp.journal;

import com.accountingapp.common.entity.OrgScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The header of a double-entry transaction. {@link #lines} must always sum
 * to zero (total debits == total credits) — enforced both in
 * {@code JournalEntryService} and by a deferred DB constraint trigger.
 */
@Getter
@Setter
@Entity
@Table(name = "journal_entries")
@NoArgsConstructor
@SuperBuilder
public class JournalEntry extends OrgScopedEntity {

    @Column(name = "entry_number", nullable = false, length = 32)
    private String entryNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.DRAFT;

    @Column(name = "memo")
    private String memo;

    @Column(name = "reference_number", length = 64)
    private String referenceNumber;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "posted_at")
    private Instant postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private JournalEntry reversalOf;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason")
    private String voidReason;

    /** Origin of auto-generated entries, e.g. "INVOICE" / "BILL" — null for manual journal entries. */
    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();

    public void addLine(JournalEntryLine line) {
        line.setJournalEntry(this);
        line.setLineNumber(lines.size() + 1);
        lines.add(line);
    }

    public boolean isEditable() {
        return status == TransactionStatus.DRAFT;
    }
}
