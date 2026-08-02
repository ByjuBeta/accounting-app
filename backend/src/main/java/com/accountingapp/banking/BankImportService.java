package com.accountingapp.banking;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.audit.AuditLogService;
import com.accountingapp.banking.BankFeedParser.ParsedTransaction;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankImportService {

    public enum FileFormat { CSV, OFX }

    public record ImportResult(int imported, int skippedDuplicates) {
    }

    private final BankFeedParser bankFeedParser;
    private final BankTransactionRepository bankTransactionRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ImportResult importFeed(UUID accountId, FileFormat format, String content) {
        UUID organizationId = OrganizationContext.getRequired();
        Account account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        List<ParsedTransaction> parsed = format == FileFormat.CSV
                ? bankFeedParser.parseCsv(content) : bankFeedParser.parseOfx(content);

        int imported = 0;
        int skipped = 0;
        for (ParsedTransaction transaction : parsed) {
            if (bankTransactionRepository.existsByAccountIdAndExternalId(account.getId(), transaction.externalId())) {
                skipped++;
                continue;
            }
            BankTransaction entity = BankTransaction.builder()
                    .account(account)
                    .transactionDate(transaction.transactionDate())
                    .amount(transaction.amount())
                    .description(transaction.description())
                    .checkNumber(transaction.checkNumber())
                    .externalId(transaction.externalId())
                    .status(BankTransactionStatus.UNMATCHED)
                    .build();
            entity.setOrganization(account.getOrganization());
            bankTransactionRepository.save(entity);
            imported++;
        }

        auditLogService.record("IMPORT", "BankTransaction", account.getId(),
                "Imported %d bank transactions for %s (%d duplicates skipped)"
                        .formatted(imported, account.getCode(), skipped), null);
        return new ImportResult(imported, skipped);
    }
}
