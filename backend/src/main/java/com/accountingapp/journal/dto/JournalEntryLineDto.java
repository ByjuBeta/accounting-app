package com.accountingapp.journal.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record JournalEntryLineDto(
        UUID id,
        int lineNumber,
        UUID accountId,
        String accountCode,
        String accountName,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String memo,
        Set<String> tags,
        boolean reconciled) {
}
