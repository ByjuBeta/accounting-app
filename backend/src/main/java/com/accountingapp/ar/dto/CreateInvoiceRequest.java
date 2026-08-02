package com.accountingapp.ar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull UUID customerId,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        String memo,
        String currencyCode,
        @NotEmpty @Valid List<InvoiceLineRequest> lines) {
}
