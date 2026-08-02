package com.accountingapp.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrganizationRequest(@NotBlank String name, String legalName, @NotBlank String timeZone) {
}
