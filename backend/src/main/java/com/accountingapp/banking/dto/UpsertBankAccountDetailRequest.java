package com.accountingapp.banking.dto;

import jakarta.validation.constraints.Pattern;

public record UpsertBankAccountDetailRequest(
        String bankName,
        @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits") String routingNumberLast4,
        @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits") String accountNumberLast4,
        String notes) {
}
