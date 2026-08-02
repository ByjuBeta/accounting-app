package com.accountingapp.ap;

import com.accountingapp.account.Account;
import com.accountingapp.common.entity.OrgScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A payment made to a {@link Vendor}, applied against one or more bills. */
@Getter
@Setter
@Entity
@Table(name = "bill_payments")
@NoArgsConstructor
@SuperBuilder
public class BillPayment extends OrgScopedEntity {

    @Column(name = "payment_number", nullable = false, length = 32)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_from_account_id", nullable = false)
    private Account paidFromAccount;

    @Column(name = "memo")
    private String memo;

    @Column(name = "reference_number", length = 64)
    private String referenceNumber;

    @OneToMany(mappedBy = "billPayment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BillPaymentApplication> applications = new ArrayList<>();

    public BigDecimal totalApplied() {
        return applications.stream().map(BillPaymentApplication::getAmountApplied).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalDiscountTaken() {
        return applications.stream().map(BillPaymentApplication::getDiscountTaken).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal unappliedAmount() {
        return amount.subtract(totalApplied());
    }
}
