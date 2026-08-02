package com.accountingapp.reports.dto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceSheetSection(String label, List<AccountBalanceLine> lines, BigDecimal total) {
}
