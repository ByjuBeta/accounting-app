package com.accountingapp.ap;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "bills")
@NoArgsConstructor
@SuperBuilder
public class Bill extends OrgScopedEntity {

    @Column(name = "bill_number", nullable = false, length = 32)
    private String billNumber;

    /** The vendor's own reference number for this bill, if they gave us one. */
    @Column(name = "vendor_reference_number", length = 64)
    private String vendorReferenceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private BillStatus status = BillStatus.DRAFT;

    @Column(name = "memo")
    private String memo;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<BillLine> lines = new ArrayList<>();

    public void addLine(BillLine line) {
        line.setBill(this);
        line.setLineNumber(lines.size() + 1);
        lines.add(line);
    }

    public void recalculateTotals() {
        subtotal = lines.stream().map(BillLine::lineSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        taxTotal = lines.stream().map(BillLine::lineTax).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.add(taxTotal);
    }

    public BigDecimal balanceDue() {
        return total.subtract(amountPaid);
    }

    public boolean isEditable() {
        return status == BillStatus.DRAFT;
    }

    public boolean isOpen() {
        return status == BillStatus.RECEIVED || status == BillStatus.PARTIALLY_PAID;
    }

    public BillStatus getEffectiveStatus(LocalDate asOfDate) {
        if (isOpen() && dueDate.isBefore(asOfDate)) {
            return BillStatus.OVERDUE;
        }
        return status;
    }

    /** Whether paying this bill in full today would still land inside the vendor's early-payment discount window. */
    public boolean isEligibleForEarlyPaymentDiscount(LocalDate paymentDate) {
        int discountDays = vendor.getEarlyPaymentDiscountDays();
        return discountDays > 0
                && vendor.getEarlyPaymentDiscountPercent().signum() > 0
                && !paymentDate.isAfter(billDate.plusDays(discountDays));
    }

    public BigDecimal earlyPaymentDiscountAmount() {
        return total.multiply(vendor.getEarlyPaymentDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}
