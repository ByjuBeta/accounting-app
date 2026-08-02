package com.accountingapp.reports.dto;

import java.math.BigDecimal;

public record CashFlowLine(String label, BigDecimal amount) {
}
