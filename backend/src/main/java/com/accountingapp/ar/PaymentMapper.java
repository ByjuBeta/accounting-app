package com.accountingapp.ar;

import com.accountingapp.ar.dto.PaymentApplicationDto;
import com.accountingapp.ar.dto.PaymentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "depositToAccountId", source = "depositToAccount.id")
    @Mapping(target = "totalApplied", expression = "java(payment.totalApplied())")
    @Mapping(target = "unappliedAmount", expression = "java(payment.unappliedAmount())")
    PaymentDto toDto(Payment payment);

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
    PaymentApplicationDto toDto(PaymentApplication application);
}
