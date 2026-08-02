package com.accountingapp.banking;

import com.accountingapp.banking.dto.ReconciliationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReconciliationMapper {

    @Mapping(target = "accountId", source = "account.id")
    ReconciliationDto toDto(Reconciliation reconciliation);
}
