package com.accountingapp.account.dto;

import com.accountingapp.account.AccountCategory;
import com.accountingapp.account.AccountStatus;
import com.accountingapp.account.AccountType;
import com.accountingapp.account.NormalBalance;
import java.util.Set;
import java.util.UUID;

public record AccountDto(
        UUID id,
        Long version,
        String code,
        String name,
        String description,
        AccountType accountType,
        AccountCategory category,
        NormalBalance normalBalance,
        UUID parentId,
        String currencyCode,
        AccountStatus status,
        Set<String> tags) {
}
