package com.accountingapp.reports.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceLine(UUID accountId, String code, String name, BigDecimal balance) {
}
