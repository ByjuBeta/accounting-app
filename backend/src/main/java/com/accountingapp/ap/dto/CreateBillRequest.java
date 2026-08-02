package com.accountingapp.ap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBillRequest(
        @NotNull UUID vendorId,
        String vendorReferenceNumber,
        @NotNull LocalDate billDate,
        LocalDate dueDate,
        String memo,
        String currencyCode,
        @NotEmpty @Valid List<BillLineRequest> lines) {
}
