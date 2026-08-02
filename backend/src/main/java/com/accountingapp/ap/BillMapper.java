package com.accountingapp.ap;

import com.accountingapp.ap.dto.BillDto;
import com.accountingapp.ap.dto.BillLineDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BillMapper {

    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorName", source = "vendor.name")
    @Mapping(target = "balanceDue", expression = "java(bill.balanceDue())")
    @Mapping(target = "effectiveStatus", expression = "java(bill.getEffectiveStatus(java.time.LocalDate.now()))")
    @Mapping(target = "eligibleForEarlyPaymentDiscountToday",
            expression = "java(bill.isEligibleForEarlyPaymentDiscount(java.time.LocalDate.now()))")
    @Mapping(target = "earlyPaymentDiscountAmount", expression = "java(bill.earlyPaymentDiscountAmount())")
    BillDto toDto(Bill bill);

    @Mapping(target = "expenseAccountId", source = "expenseAccount.id")
    @Mapping(target = "expenseAccountCode", source = "expenseAccount.code")
    @Mapping(target = "lineSubtotal", expression = "java(line.lineSubtotal())")
    @Mapping(target = "lineTax", expression = "java(line.lineTax())")
    @Mapping(target = "lineTotal", expression = "java(line.lineTotal())")
    BillLineDto toDto(BillLine line);
}
