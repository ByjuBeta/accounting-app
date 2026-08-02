package com.accountingapp.ar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PaymentDto(
        UUID id,
        Long version,
        String paymentNumber,
        UUID customerId,
        String customerName,
        LocalDate paymentDate,
        BigDecimal amount,
        UUID depositToAccountId,
        String memo,
        String referenceNumber,
        BigDecimal totalApplied,
        BigDecimal unappliedAmount,
        List<PaymentApplicationDto> applications) {
}
