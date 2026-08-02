package com.accountingapp.ap.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BillPaymentDto(
        UUID id,
        Long version,
        String paymentNumber,
        UUID vendorId,
        String vendorName,
        LocalDate paymentDate,
        BigDecimal amount,
        UUID paidFromAccountId,
        String memo,
        String referenceNumber,
        BigDecimal totalApplied,
        BigDecimal totalDiscountTaken,
        BigDecimal unappliedAmount,
        List<BillPaymentApplicationDto> applications) {
}
