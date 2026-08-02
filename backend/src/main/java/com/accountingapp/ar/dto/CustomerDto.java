package com.accountingapp.ar.dto;

import com.accountingapp.ar.CustomerStatus;
import java.util.UUID;

public record CustomerDto(
        UUID id,
        Long version,
        String name,
        String companyName,
        String email,
        String phone,
        AddressDto billingAddress,
        int paymentTermsDays,
        boolean taxExempt,
        String notes,
        CustomerStatus status) {
}
