package com.accountingapp.organization.dto;

import java.util.UUID;

public record OrganizationDto(UUID id, String name, String legalName, String baseCurrencyCode, String timeZone, boolean active) {
}
