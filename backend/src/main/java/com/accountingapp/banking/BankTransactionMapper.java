package com.accountingapp.banking;

import com.accountingapp.banking.dto.BankTransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BankTransactionMapper {

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "matchedJournalEntryLineId",
            expression = "java(transaction.getMatchedJournalEntryLine() != null ? transaction.getMatchedJournalEntryLine().getId() : null)")
    BankTransactionDto toDto(BankTransaction transaction);
}
