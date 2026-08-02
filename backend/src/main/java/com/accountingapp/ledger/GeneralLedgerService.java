package com.accountingapp.ledger;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.journal.JournalEntryLineRepository;
import com.accountingapp.journal.JournalEntryLineRepository.AccountTotals;
import com.accountingapp.journal.JournalEntryLineRepository.AccountTotalsByAccount;
import com.accountingapp.journal.JournalEntryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeneralLedgerService {

    private final AccountRepository accountRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public AccountBalance getAccountBalance(UUID accountId, LocalDate asOfDate) {
        UUID organizationId = OrganizationContext.getRequired();
        Account account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        AccountTotals totals = journalEntryLineRepository.sumForAccountAsOf(
                organizationId, accountId, asOfDate, JournalEntryService.balanceAffectingStatuses());
        BigDecimal debit = totals != null ? totals.getTotalDebit() : BigDecimal.ZERO;
        BigDecimal credit = totals != null ? totals.getTotalCredit() : BigDecimal.ZERO;
        return AccountBalance.of(account.getId(), account.getCode(), account.getName(), account.getAccountType(), debit, credit);
    }

    public TrialBalance getTrialBalance(LocalDate asOfDate) {
        UUID organizationId = OrganizationContext.getRequired();
        List<Account> accounts = accountRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
        Map<UUID, AccountTotalsByAccount> totalsByAccount = journalEntryLineRepository
                .sumForAllAccountsAsOf(organizationId, asOfDate, JournalEntryService.balanceAffectingStatuses())
                .stream()
                .collect(java.util.stream.Collectors.toMap(AccountTotalsByAccount::getAccountId, Function.identity()));

        List<AccountBalance> rows = accounts.stream()
                .map(account -> {
                    AccountTotalsByAccount totals = totalsByAccount.get(account.getId());
                    BigDecimal debit = totals != null ? totals.getTotalDebit() : BigDecimal.ZERO;
                    BigDecimal credit = totals != null ? totals.getTotalCredit() : BigDecimal.ZERO;
                    return AccountBalance.of(account.getId(), account.getCode(), account.getName(), account.getAccountType(), debit, credit);
                })
                .filter(row -> row.totalDebit().signum() != 0 || row.totalCredit().signum() != 0)
                .toList();

        BigDecimal totalDebit = rows.stream().map(AccountBalance::totalDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = rows.stream().map(AccountBalance::totalCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TrialBalance(asOfDate, rows, totalDebit, totalCredit);
    }
}
