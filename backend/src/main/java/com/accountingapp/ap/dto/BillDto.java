package com.accountingapp.ap.dto;

import com.accountingapp.ap.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BillDto(
        UUID id,
        Long version,
        String billNumber,
        String vendorReferenceNumber,
        UUID vendorId,
        String vendorName,
        LocalDate billDate,
        LocalDate dueDate,
        BillStatus status,
        BillStatus effectiveStatus,
        String memo,
        String currencyCode,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        boolean eligibleForEarlyPaymentDiscountToday,
        BigDecimal earlyPaymentDiscountAmount,
        List<BillLineDto> lines) {
}
