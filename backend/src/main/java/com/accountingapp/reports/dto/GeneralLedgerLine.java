package com.accountingapp.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GeneralLedgerLine(
        UUID journalEntryLineId,
        UUID journalEntryId,
        String entryNumber,
        LocalDate entryDate,
        String memo,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal runningBalance) {
}
