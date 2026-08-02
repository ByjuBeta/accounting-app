package com.accountingapp.ap.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BillPaymentApplicationDto(
        UUID billId, String billNumber, BigDecimal amountApplied, BigDecimal discountTaken) {
}
