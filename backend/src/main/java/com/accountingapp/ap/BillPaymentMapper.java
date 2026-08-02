package com.accountingapp.ap;

import com.accountingapp.ap.dto.BillPaymentApplicationDto;
import com.accountingapp.ap.dto.BillPaymentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BillPaymentMapper {

    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorName", source = "vendor.name")
    @Mapping(target = "paidFromAccountId", source = "paidFromAccount.id")
    @Mapping(target = "totalApplied", expression = "java(billPayment.totalApplied())")
    @Mapping(target = "totalDiscountTaken", expression = "java(billPayment.totalDiscountTaken())")
    @Mapping(target = "unappliedAmount", expression = "java(billPayment.unappliedAmount())")
    BillPaymentDto toDto(BillPayment billPayment);

    @Mapping(target = "billId", source = "bill.id")
    @Mapping(target = "billNumber", source = "bill.billNumber")
    BillPaymentApplicationDto toDto(BillPaymentApplication application);
}
