package com.accountingapp.journal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpdateJournalEntryRequest(
        @NotNull LocalDate entryDate,
        String memo,
        String referenceNumber,
        @NotEmpty @Size(min = 2, message = "A journal entry needs at least two lines")
        @Valid List<JournalEntryLineRequest> lines) {
}
