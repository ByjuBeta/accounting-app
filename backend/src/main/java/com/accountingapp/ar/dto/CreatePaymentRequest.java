package com.accountingapp.ar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID customerId,
        @NotNull LocalDate paymentDate,
        @NotNull @Positive BigDecimal amount,
        @NotNull UUID depositToAccountId,
        String memo,
        String referenceNumber,
        @Valid List<PaymentApplicationRequest> applications) {
}
