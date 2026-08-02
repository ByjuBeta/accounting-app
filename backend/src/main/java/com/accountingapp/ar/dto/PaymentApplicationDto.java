package com.accountingapp.ar.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentApplicationDto(UUID invoiceId, String invoiceNumber, BigDecimal amountApplied) {
}
