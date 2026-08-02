package com.accountingapp.ap.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BillLineDto(
        UUID id,
        int lineNumber,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        UUID expenseAccountId,
        String expenseAccountCode,
        BigDecimal lineSubtotal,
        BigDecimal lineTax,
        BigDecimal lineTotal) {
}
