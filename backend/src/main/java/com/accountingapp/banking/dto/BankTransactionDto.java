package com.accountingapp.banking.dto;

import com.accountingapp.banking.BankTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankTransactionDto(
        UUID id,
        UUID accountId,
        LocalDate transactionDate,
        BigDecimal amount,
        String description,
        String checkNumber,
        BankTransactionStatus status,
        UUID matchedJournalEntryLineId) {
}
