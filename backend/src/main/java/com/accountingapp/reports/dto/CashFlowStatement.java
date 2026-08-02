package com.accountingapp.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Investing and financing sections are computed identically regardless of
 * {@code method} — only the operating section differs between direct
 * (itemized by counterparty category) and indirect (net income plus working
 * -capital adjustments).
 */
public record CashFlowStatement(
        LocalDate fromDate,
        LocalDate toDate,
        CashFlowMethod method,
        List<CashFlowLine> operatingLines,
        BigDecimal operatingTotal,
        List<CashFlowLine> investingLines,
        BigDecimal investingTotal,
        List<CashFlowLine> financingLines,
        BigDecimal financingTotal,
        BigDecimal netChangeInCash,
        BigDecimal beginningCash,
        BigDecimal endingCash) {
}
