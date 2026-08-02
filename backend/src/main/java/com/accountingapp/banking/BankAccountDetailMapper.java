package com.accountingapp.banking;

import com.accountingapp.banking.dto.BankAccountDetailDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BankAccountDetailMapper {

    @Mapping(target = "accountId", source = "account.id")
    BankAccountDetailDto toDto(BankAccountDetail detail);
}
