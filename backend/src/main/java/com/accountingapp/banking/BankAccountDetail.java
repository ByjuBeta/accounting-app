package com.accountingapp.banking;

import com.accountingapp.account.Account;
import com.accountingapp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Bank-specific metadata for a {@link Account} of type BANK/CREDIT_CARD.
 * Kept off the core Account entity since most account types don't need it.
 * Only the last 4 digits of routing/account numbers are stored — full
 * numbers have no use case here and shouldn't be persisted unencrypted.
 */
@Getter
@Setter
@Entity
@Table(name = "bank_account_details")
@NoArgsConstructor
@SuperBuilder
public class BankAccountDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "routing_number_last4", length = 4)
    private String routingNumberLast4;

    @Column(name = "account_number_last4", length = 4)
    private String accountNumberLast4;

    @Column(name = "notes")
    private String notes;
}
