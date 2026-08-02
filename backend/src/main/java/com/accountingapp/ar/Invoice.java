package com.accountingapp.ar;

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
@Table(name = "invoices")
@NoArgsConstructor
@SuperBuilder
public class Invoice extends OrgScopedEntity {

    @Column(name = "invoice_number", nullable = false, length = 32)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

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

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    public void addLine(InvoiceLine line) {
        line.setInvoice(this);
        line.setLineNumber(lines.size() + 1);
        lines.add(line);
    }

    public void recalculateTotals() {
        subtotal = lines.stream().map(InvoiceLine::lineSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        taxTotal = lines.stream().map(InvoiceLine::lineTax).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.add(taxTotal);
    }

    public BigDecimal balanceDue() {
        return total.subtract(amountPaid);
    }

    public boolean isEditable() {
        return status == InvoiceStatus.DRAFT;
    }

    public boolean isOpen() {
        return status == InvoiceStatus.SENT || status == InvoiceStatus.PARTIALLY_PAID;
    }

    /** Overdue is derived, not stored: an open invoice past its due date reads as overdue without a background job. */
    public InvoiceStatus getEffectiveStatus(LocalDate asOfDate) {
        if (isOpen() && dueDate.isBefore(asOfDate)) {
            return InvoiceStatus.OVERDUE;
        }
        return status;
    }
}
