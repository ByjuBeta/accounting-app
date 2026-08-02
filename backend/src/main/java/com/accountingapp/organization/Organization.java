package com.accountingapp.organization;

import com.accountingapp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A tenant. Every operational record (accounts, transactions, invoices, ...)
 * is scoped to exactly one organization.
 */
@Getter
@Setter
@Entity
@Table(name = "organizations")
@NoArgsConstructor
@SuperBuilder
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "base_currency_code", nullable = false, length = 3)
    private String baseCurrencyCode;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "active", nullable = false)
    private boolean active;
}
