package com.accountingapp.banking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OutstandingLineDto(
        UUID journalEntryLineId, UUID journalEntryId, String entryNumber,
        LocalDate entryDate, String description, BigDecimal amount) {
}
