package com.accountingapp.banking.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StartReconciliationRequest(@NotNull LocalDate statementDate, @NotNull BigDecimal statementEndingBalance) {
}
