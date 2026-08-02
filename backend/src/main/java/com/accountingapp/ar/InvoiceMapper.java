package com.accountingapp.ar;

import com.accountingapp.ar.dto.InvoiceDto;
import com.accountingapp.ar.dto.InvoiceLineDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "balanceDue", expression = "java(invoice.balanceDue())")
    @Mapping(target = "effectiveStatus", expression = "java(invoice.getEffectiveStatus(java.time.LocalDate.now()))")
    InvoiceDto toDto(Invoice invoice);

    @Mapping(target = "incomeAccountId", source = "incomeAccount.id")
    @Mapping(target = "incomeAccountCode", source = "incomeAccount.code")
    @Mapping(target = "lineSubtotal", expression = "java(line.lineSubtotal())")
    @Mapping(target = "lineTax", expression = "java(line.lineTax())")
    @Mapping(target = "lineTotal", expression = "java(line.lineTotal())")
    InvoiceLineDto toDto(InvoiceLine line);
}
