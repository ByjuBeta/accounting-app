package com.accountingapp.ap;

import com.accountingapp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** How much of a {@link BillPayment} was applied against one {@link Bill}, plus any early-payment discount taken. */
@Getter
@Setter
@Entity
@Table(name = "bill_payment_applications")
@NoArgsConstructor
@SuperBuilder
public class BillPaymentApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_payment_id", nullable = false)
    private BillPayment billPayment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "amount_applied", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountApplied;

    @Column(name = "discount_taken", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal discountTaken = BigDecimal.ZERO;

    /** Total relief to the bill's balance: cash paid plus any discount taken. */
    public BigDecimal totalAppliedToBalance() {
        return amountApplied.add(discountTaken);
    }
}
