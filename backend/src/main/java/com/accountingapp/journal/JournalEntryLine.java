package com.accountingapp.journal;

import com.accountingapp.account.Account;
import com.accountingapp.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** One debit or credit leg of a {@link JournalEntry}. Exactly one of debit/credit is non-zero. */
@Getter
@Setter
@Entity
@Table(name = "journal_entry_lines")
@NoArgsConstructor
@SuperBuilder
public class JournalEntryLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "memo")
    private String memo;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "journal_entry_line_tags", joinColumns = @JoinColumn(name = "journal_entry_line_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "reconciled", nullable = false)
    @Builder.Default
    private boolean reconciled = false;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    public BigDecimal signedAmount() {
        return debitAmount.subtract(creditAmount);
    }
}
