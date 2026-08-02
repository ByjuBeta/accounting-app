package com.accountingapp.banking;

import com.accountingapp.account.Account;
import com.accountingapp.common.entity.OrgScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** One statement-reconciliation session for a bank/credit-card account. */
@Getter
@Setter
@Entity
@Table(name = "reconciliations")
@NoArgsConstructor
@SuperBuilder
public class Reconciliation extends OrgScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "beginning_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance;

    @Column(name = "statement_ending_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal statementEndingBalance;

    @Column(name = "cleared_balance", precision = 19, scale = 2)
    private BigDecimal clearedBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.IN_PROGRESS;

    @Column(name = "completed_at")
    private Instant completedAt;
}
