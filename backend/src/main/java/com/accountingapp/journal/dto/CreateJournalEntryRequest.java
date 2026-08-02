package com.accountingapp.journal.dto;

import com.accountingapp.journal.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateJournalEntryRequest(
        @NotNull LocalDate entryDate,
        @NotNull TransactionType transactionType,
        String memo,
        String referenceNumber,
        String currencyCode,
        @NotEmpty @Size(min = 2, message = "A journal entry needs at least two lines")
        @Valid List<JournalEntryLineRequest> lines) {
}
