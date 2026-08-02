package com.accountingapp.account;

import com.accountingapp.account.dto.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "category", expression = "java(account.getCategory())")
    @Mapping(target = "normalBalance", expression = "java(account.getNormalBalance())")
    @Mapping(target = "parentId", expression = "java(account.getParent() != null ? account.getParent().getId() : null)")
    AccountDto toDto(Account account);
}
