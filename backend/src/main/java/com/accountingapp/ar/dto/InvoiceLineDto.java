package com.accountingapp.ar.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineDto(
        UUID id,
        int lineNumber,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        UUID incomeAccountId,
        String incomeAccountCode,
        BigDecimal lineSubtotal,
        BigDecimal lineTax,
        BigDecimal lineTotal) {
}
