package com.accountingapp.journal.dto;

import com.accountingapp.journal.TransactionStatus;
import com.accountingapp.journal.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JournalEntryDto(
        UUID id,
        Long version,
        String entryNumber,
        LocalDate entryDate,
        TransactionType transactionType,
        TransactionStatus status,
        String memo,
        String referenceNumber,
        String currencyCode,
        Instant postedAt,
        UUID reversalOfId,
        Instant voidedAt,
        String voidReason,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        List<JournalEntryLineDto> lines) {
}
