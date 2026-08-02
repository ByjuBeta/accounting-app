package com.accountingapp.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IncomeStatement(
        LocalDate fromDate,
        LocalDate toDate,
        List<AccountBalanceLine> revenue,
        List<AccountBalanceLine> expenses,
        BigDecimal totalRevenue,
        BigDecimal totalExpense,
        BigDecimal netIncome) {
}
