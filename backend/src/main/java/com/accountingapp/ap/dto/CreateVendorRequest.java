package com.accountingapp.ap.dto;

import com.accountingapp.common.dto.AddressDto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateVendorRequest(
        @NotBlank String name,
        String companyName,
        @Email String email,
        String phone,
        AddressDto address,
        @PositiveOrZero Integer paymentTermsDays,
        @DecimalMin("0") @DecimalMax("100") BigDecimal earlyPaymentDiscountPercent,
        @PositiveOrZero Integer earlyPaymentDiscountDays,
        String taxId,
        String notes) {
}
