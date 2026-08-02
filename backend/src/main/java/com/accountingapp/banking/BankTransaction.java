package com.accountingapp.banking;

import com.accountingapp.account.Account;
import com.accountingapp.common.entity.OrgScopedEntity;
import com.accountingapp.journal.JournalEntryLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A single line from an imported bank feed (CSV/OFX), before it's reconciled
 * against the books. {@code amount} follows bank-feed sign convention:
 * positive = money in, negative = money out.
 */
@Getter
@Setter
@Entity
@Table(name = "bank_transactions")
@NoArgsConstructor
@SuperBuilder
public class BankTransaction extends OrgScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "check_number", length = 32)
    private String checkNumber;

    /** Dedupe key: OFX FITID, or a stable hash of (date, amount, description) for CSV imports. */
    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private BankTransactionStatus status = BankTransactionStatus.UNMATCHED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_journal_entry_line_id")
    private JournalEntryLine matchedJournalEntryLine;
}
