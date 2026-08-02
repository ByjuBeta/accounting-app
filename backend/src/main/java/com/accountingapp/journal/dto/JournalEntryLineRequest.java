package com.accountingapp.journal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record JournalEntryLineRequest(
        @NotNull UUID accountId,
        @NotNull @PositiveOrZero BigDecimal debitAmount,
        @NotNull @PositiveOrZero BigDecimal creditAmount,
        String memo,
        Set<String> tags) {
}
