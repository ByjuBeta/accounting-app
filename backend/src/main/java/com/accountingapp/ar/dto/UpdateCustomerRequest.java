package com.accountingapp.ar.dto;

import com.accountingapp.ar.CustomerStatus;
import com.accountingapp.common.dto.AddressDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateCustomerRequest(
        @NotBlank String name,
        String companyName,
        @Email String email,
        String phone,
        AddressDto billingAddress,
        @PositiveOrZero Integer paymentTermsDays,
        boolean taxExempt,
        String notes,
        CustomerStatus status) {
}
