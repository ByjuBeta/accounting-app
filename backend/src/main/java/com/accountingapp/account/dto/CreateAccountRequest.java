package com.accountingapp.account.dto;

import com.accountingapp.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CreateAccountRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull AccountType accountType,
        UUID parentId,
        String currencyCode,
        Set<String> tags,
        @PositiveOrZero BigDecimal openingBalance,
        LocalDate openingBalanceDate) {
}
