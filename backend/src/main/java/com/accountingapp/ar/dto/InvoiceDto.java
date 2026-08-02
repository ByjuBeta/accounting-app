package com.accountingapp.ar.dto;

import com.accountingapp.ar.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceDto(
        UUID id,
        Long version,
        String invoiceNumber,
        UUID customerId,
        String customerName,
        LocalDate invoiceDate,
        LocalDate dueDate,
        InvoiceStatus status,
        InvoiceStatus effectiveStatus,
        String memo,
        String currencyCode,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        List<InvoiceLineDto> lines) {
}
