package com.accountingapp.ap.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record BillPaymentApplicationRequest(
        @NotNull UUID billId,
        @NotNull @Positive BigDecimal amount,
        @PositiveOrZero BigDecimal discountTaken) {
}
