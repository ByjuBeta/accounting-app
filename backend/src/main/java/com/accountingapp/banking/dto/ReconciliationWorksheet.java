package com.accountingapp.banking.dto;

import com.accountingapp.banking.ReconciliationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReconciliationWorksheet(
        UUID reconciliationId,
        UUID accountId,
        LocalDate statementDate,
        ReconciliationStatus status,
        BigDecimal beginningBalance,
        BigDecimal statementEndingBalance,
        BigDecimal clearedTotal,
        BigDecimal projectedEndingBalance,
        BigDecimal difference,
        int clearedTransactionCount,
        List<OutstandingLineDto> outstandingLines) {

    public boolean isBalanced() {
        return difference.signum() == 0;
    }
}
