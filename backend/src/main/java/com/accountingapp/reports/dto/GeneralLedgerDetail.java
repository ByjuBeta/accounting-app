package com.accountingapp.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GeneralLedgerDetail(
        UUID accountId,
        String accountCode,
        String accountName,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal beginningBalance,
        List<GeneralLedgerLine> lines,
        BigDecimal endingBalance) {
}
