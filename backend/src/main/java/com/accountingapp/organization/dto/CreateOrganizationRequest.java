package com.accountingapp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank String name,
        String legalName,
        @NotBlank @Size(min = 3, max = 3) String baseCurrencyCode,
        @NotBlank String timeZone) {
}
