package com.accountingapp.account.dto;

import com.accountingapp.account.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;

public record UpdateAccountRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        UUID parentId,
        AccountStatus status,
        Set<String> tags) {
}
