package com.accountingapp.journal;

import com.accountingapp.journal.dto.JournalEntryDto;
import com.accountingapp.journal.dto.JournalEntryLineDto;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JournalEntryMapper {

    @Mapping(target = "reversalOfId",
            expression = "java(entry.getReversalOf() != null ? entry.getReversalOf().getId() : null)")
    @Mapping(target = "totalDebit", expression = "java(totalDebit(entry))")
    @Mapping(target = "totalCredit", expression = "java(totalCredit(entry))")
    JournalEntryDto toDto(JournalEntry entry);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountCode", source = "account.code")
    @Mapping(target = "accountName", source = "account.name")
    JournalEntryLineDto toDto(JournalEntryLine line);

    default BigDecimal totalDebit(JournalEntry entry) {
        return entry.getLines().stream().map(JournalEntryLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal totalCredit(JournalEntry entry) {
        return entry.getLines().stream().map(JournalEntryLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    List<JournalEntryLineDto> toLineDtoList(List<JournalEntryLine> lines);
}
