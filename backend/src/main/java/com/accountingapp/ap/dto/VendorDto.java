package com.accountingapp.ap.dto;

import com.accountingapp.ap.VendorStatus;
import com.accountingapp.common.dto.AddressDto;
import java.math.BigDecimal;
import java.util.UUID;

public record VendorDto(
        UUID id,
        Long version,
        String name,
        String companyName,
        String email,
        String phone,
        AddressDto address,
        int paymentTermsDays,
        BigDecimal earlyPaymentDiscountPercent,
        int earlyPaymentDiscountDays,
        String taxId,
        String notes,
        VendorStatus status) {
}
