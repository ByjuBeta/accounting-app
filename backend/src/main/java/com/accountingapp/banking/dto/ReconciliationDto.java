package com.accountingapp.banking.dto;

import com.accountingapp.banking.ReconciliationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReconciliationDto(
        UUID id,
        UUID accountId,
        LocalDate statementDate,
        BigDecimal beginningBalance,
        BigDecimal statementEndingBalance,
        BigDecimal clearedBalance,
        ReconciliationStatus status,
        Instant completedAt) {
}
