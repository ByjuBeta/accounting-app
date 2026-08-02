package com.accountingapp.banking.dto;

import java.util.UUID;

public record BankAccountDetailDto(
        UUID id, UUID accountId, String bankName, String routingNumberLast4, String accountNumberLast4, String notes) {
}
