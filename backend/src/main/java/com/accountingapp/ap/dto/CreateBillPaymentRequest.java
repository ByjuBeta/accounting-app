package com.accountingapp.ap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBillPaymentRequest(
        @NotNull UUID vendorId,
        @NotNull LocalDate paymentDate,
        @NotNull @Positive BigDecimal amount,
        @NotNull UUID paidFromAccountId,
        String memo,
        String referenceNumber,
        @Valid List<BillPaymentApplicationRequest> applications) {
}
