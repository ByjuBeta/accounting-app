package com.accountingapp.ap;

import com.accountingapp.common.entity.OrgScopedEntity;
import com.accountingapp.common.value.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "vendors")
@NoArgsConstructor
@SuperBuilder
public class Vendor extends OrgScopedEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Embedded
    private Address address;

    @Column(name = "payment_terms_days", nullable = false)
    @Builder.Default
    private int paymentTermsDays = 30;

    /** "2/10 net 30"-style early-payment discount: {@code discountPercent}% off if paid within {@code discountDays}. */
    @Column(name = "early_payment_discount_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal earlyPaymentDiscountPercent = BigDecimal.ZERO;

    @Column(name = "early_payment_discount_days", nullable = false)
    @Builder.Default
    private int earlyPaymentDiscountDays = 0;

    @Column(name = "tax_id", length = 64)
    private String taxId;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private VendorStatus status = VendorStatus.ACTIVE;
}
