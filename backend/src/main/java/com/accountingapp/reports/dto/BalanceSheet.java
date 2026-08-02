package com.accountingapp.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Assets = Liabilities + Equity, where {@code equity} already includes
 * {@code netIncomeToDate} as its own line — there's no period-close step in
 * this system that would otherwise roll accumulated net income into a
 * retained-earnings account, so the report folds it in itself.
 */
public record BalanceSheet(
        LocalDate asOfDate,
        BalanceSheetSection assets,
        BalanceSheetSection liabilities,
        BalanceSheetSection equity,
        BigDecimal netIncomeToDate,
        BigDecimal totalLiabilitiesAndEquity,
        boolean balanced) {
}
