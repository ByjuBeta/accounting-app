package com.accountingapp.reports;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountCategory;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountType;
import com.accountingapp.account.NormalBalance;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.journal.JournalEntryLine;
import com.accountingapp.journal.JournalEntryLineRepository;
import com.accountingapp.journal.JournalEntryLineRepository.AccountTotalsByAccount;
import com.accountingapp.journal.JournalEntryService;
import com.accountingapp.reports.dto.AccountBalanceLine;
import com.accountingapp.reports.dto.BalanceSheet;
import com.accountingapp.reports.dto.BalanceSheetSection;
import com.accountingapp.reports.dto.CashFlowLine;
import com.accountingapp.reports.dto.CashFlowMethod;
import com.accountingapp.reports.dto.CashFlowStatement;
import com.accountingapp.reports.dto.GeneralLedgerDetail;
import com.accountingapp.reports.dto.GeneralLedgerLine;
import com.accountingapp.reports.dto.IncomeStatement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportsService {

    private static final Set<AccountType> CASH_TYPES = EnumSet.of(AccountType.BANK, AccountType.CASH);
    private static final Set<AccountType> ASSET_SIDE_WORKING_CAPITAL =
            EnumSet.of(AccountType.ACCOUNTS_RECEIVABLE, AccountType.OTHER_CURRENT_ASSET);
    private static final Set<AccountType> LIABILITY_SIDE_WORKING_CAPITAL =
            EnumSet.of(AccountType.ACCOUNTS_PAYABLE, AccountType.OTHER_CURRENT_LIABILITY);

    private final AccountRepository accountRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public BalanceSheet getBalanceSheet(LocalDate asOfDate) {
        UUID organizationId = OrganizationContext.getRequired();
        List<Account> accounts = accountRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
        Map<UUID, AccountTotalsByAccount> totals = totalsAsOfByAccount(organizationId, asOfDate);

        BalanceSheetSection assets = buildSection("Assets", accounts, totals, AccountCategory.ASSET);
        BalanceSheetSection liabilities = buildSection("Liabilities", accounts, totals, AccountCategory.LIABILITY);
        BalanceSheetSection equityAccounts = buildSection("Equity", accounts, totals, AccountCategory.EQUITY);

        BigDecimal netIncomeToDate = netIncome(accounts, totals);
        List<AccountBalanceLine> equityLines = new ArrayList<>(equityAccounts.lines());
        equityLines.add(new AccountBalanceLine(null, null, "Net Income (current)", netIncomeToDate));
        BalanceSheetSection equity = new BalanceSheetSection(
                "Equity", equityLines, equityAccounts.total().add(netIncomeToDate));

        BigDecimal totalLiabilitiesAndEquity = liabilities.total().add(equity.total());
        boolean balanced = assets.total().compareTo(totalLiabilitiesAndEquity) == 0;

        return new BalanceSheet(asOfDate, assets, liabilities, equity, netIncomeToDate, totalLiabilitiesAndEquity, balanced);
    }

    public IncomeStatement getIncomeStatement(LocalDate fromDate, LocalDate toDate) {
        UUID organizationId = OrganizationContext.getRequired();
        List<Account> accounts = accountRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
        Map<UUID, AccountTotalsByAccount> totals = journalEntryLineRepository
                .sumForAllAccountsInRange(organizationId, fromDate, toDate, JournalEntryService.balanceAffectingStatuses())
                .stream().collect(Collectors.toMap(AccountTotalsByAccount::getAccountId, t -> t));

        List<AccountBalanceLine> revenue = balanceLines(accounts, totals, AccountCategory.REVENUE);
        List<AccountBalanceLine> expenses = balanceLines(accounts, totals, AccountCategory.EXPENSE);
        BigDecimal totalRevenue = sumLines(revenue);
        BigDecimal totalExpense = sumLines(expenses);

        return new IncomeStatement(fromDate, toDate, revenue, expenses, totalRevenue, totalExpense,
                totalRevenue.subtract(totalExpense));
    }

    public CashFlowStatement getCashFlowStatement(LocalDate fromDate, LocalDate toDate, CashFlowMethod method) {
        UUID organizationId = OrganizationContext.getRequired();
        List<Account> cashAccounts = accountRepository.findByOrganizationIdOrderByCodeAsc(organizationId).stream()
                .filter(a -> CASH_TYPES.contains(a.getAccountType()))
                .toList();
        List<UUID> cashAccountIds = cashAccounts.stream().map(Account::getId).toList();

        BigDecimal beginningCash = sumDebitNormalBalanceAsOf(organizationId, cashAccountIds, fromDate.minusDays(1));
        BigDecimal endingCash = sumDebitNormalBalanceAsOf(organizationId, cashAccountIds, toDate);

        CategorizedCashActivity activity = categorizeCashActivity(organizationId, cashAccountIds, fromDate, toDate);

        List<CashFlowLine> operatingLines;
        BigDecimal operatingTotal;
        if (method == CashFlowMethod.DIRECT) {
            operatingLines = toLines(activity.operating());
            operatingTotal = sumValues(activity.operating());
        } else {
            operatingLines = buildIndirectOperatingLines(organizationId, fromDate, toDate);
            operatingTotal = operatingLines.stream().map(CashFlowLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        List<CashFlowLine> investingLines = toLines(activity.investing());
        BigDecimal investingTotal = sumValues(activity.investing());
        List<CashFlowLine> financingLines = toLines(activity.financing());
        BigDecimal financingTotal = sumValues(activity.financing());

        return new CashFlowStatement(fromDate, toDate, method,
                operatingLines, operatingTotal, investingLines, investingTotal, financingLines, financingTotal,
                endingCash.subtract(beginningCash), beginningCash, endingCash);
    }

    public GeneralLedgerDetail getGeneralLedgerDetail(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        UUID organizationId = OrganizationContext.getRequired();
        Account account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        BigDecimal beginningBalance = sumDebitNormalBalanceAsOf(organizationId, List.of(accountId), fromDate.minusDays(1));
        if (account.getNormalBalance() == NormalBalance.CREDIT) {
            // sumDebitNormalBalanceAsOf assumes debit-normal; flip sign for credit-normal accounts.
            beginningBalance = beginningBalance.negate();
        }

        List<JournalEntryLine> lines = journalEntryLineRepository.findByAccountIdsAndDateRange(
                organizationId, List.of(accountId), fromDate, toDate, JournalEntryService.balanceAffectingStatuses());

        List<GeneralLedgerLine> glLines = new ArrayList<>();
        BigDecimal runningBalance = beginningBalance;
        for (JournalEntryLine line : lines) {
            BigDecimal delta = account.getNormalBalance() == NormalBalance.DEBIT
                    ? line.signedAmount() : line.signedAmount().negate();
            runningBalance = runningBalance.add(delta);
            glLines.add(new GeneralLedgerLine(
                    line.getId(), line.getJournalEntry().getId(), line.getJournalEntry().getEntryNumber(),
                    line.getJournalEntry().getEntryDate(), line.getMemo(),
                    line.getDebitAmount(), line.getCreditAmount(), runningBalance));
        }

        return new GeneralLedgerDetail(account.getId(), account.getCode(), account.getName(),
                fromDate, toDate, beginningBalance, glLines, runningBalance);
    }

    // -- helpers --------------------------------------------------------

    private Map<UUID, AccountTotalsByAccount> totalsAsOfByAccount(UUID organizationId, LocalDate asOfDate) {
        return journalEntryLineRepository.sumForAllAccountsAsOf(
                        organizationId, asOfDate, JournalEntryService.balanceAffectingStatuses())
                .stream().collect(Collectors.toMap(AccountTotalsByAccount::getAccountId, t -> t));
    }

    private BalanceSheetSection buildSection(
            String label, List<Account> accounts, Map<UUID, AccountTotalsByAccount> totals, AccountCategory category) {
        List<AccountBalanceLine> lines = balanceLines(accounts, totals, category);
        return new BalanceSheetSection(label, lines, sumLines(lines));
    }

    private List<AccountBalanceLine> balanceLines(
            List<Account> accounts, Map<UUID, AccountTotalsByAccount> totals, AccountCategory category) {
        return accounts.stream()
                .filter(a -> a.getCategory() == category)
                .map(a -> new AccountBalanceLine(a.getId(), a.getCode(), a.getName(), signedBalance(a, totals.get(a.getId()))))
                .filter(l -> l.balance().signum() != 0)
                .sorted(Comparator.comparing(AccountBalanceLine::code, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private BigDecimal netIncome(List<Account> accounts, Map<UUID, AccountTotalsByAccount> totals) {
        BigDecimal revenue = sumLines(balanceLinesIncludingZero(accounts, totals, AccountCategory.REVENUE));
        BigDecimal expense = sumLines(balanceLinesIncludingZero(accounts, totals, AccountCategory.EXPENSE));
        return revenue.subtract(expense);
    }

    private List<AccountBalanceLine> balanceLinesIncludingZero(
            List<Account> accounts, Map<UUID, AccountTotalsByAccount> totals, AccountCategory category) {
        return accounts.stream()
                .filter(a -> a.getCategory() == category)
                .map(a -> new AccountBalanceLine(a.getId(), a.getCode(), a.getName(), signedBalance(a, totals.get(a.getId()))))
                .toList();
    }

    private BigDecimal signedBalance(Account account, AccountTotalsByAccount totals) {
        BigDecimal debit = totals != null ? totals.getTotalDebit() : BigDecimal.ZERO;
        BigDecimal credit = totals != null ? totals.getTotalCredit() : BigDecimal.ZERO;
        return account.getNormalBalance() == NormalBalance.DEBIT ? debit.subtract(credit) : credit.subtract(debit);
    }

    private BigDecimal sumLines(List<AccountBalanceLine> lines) {
        return lines.stream().map(AccountBalanceLine::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sums debit-normal balance across the given (cash) accounts as of a date. */
    private BigDecimal sumDebitNormalBalanceAsOf(UUID organizationId, List<UUID> accountIds, LocalDate asOfDate) {
        Map<UUID, AccountTotalsByAccount> totals = totalsAsOfByAccount(organizationId, asOfDate);
        BigDecimal sum = BigDecimal.ZERO;
        for (UUID accountId : accountIds) {
            AccountTotalsByAccount t = totals.get(accountId);
            if (t != null) {
                sum = sum.add(t.getTotalDebit()).subtract(t.getTotalCredit());
            }
        }
        return sum;
    }

    private record CategorizedCashActivity(
            Map<String, BigDecimal> operating, Map<String, BigDecimal> investing, Map<String, BigDecimal> financing) {
    }

    private CategorizedCashActivity categorizeCashActivity(
            UUID organizationId, List<UUID> cashAccountIds, LocalDate fromDate, LocalDate toDate) {
        Map<String, BigDecimal> operating = new LinkedHashMap<>();
        Map<String, BigDecimal> investing = new LinkedHashMap<>();
        Map<String, BigDecimal> financing = new LinkedHashMap<>();

        List<JournalEntryLine> cashLines = journalEntryLineRepository.findByAccountIdsAndDateRange(
                organizationId, cashAccountIds, fromDate, toDate, JournalEntryService.balanceAffectingStatuses());

        for (JournalEntryLine cashLine : cashLines) {
            JournalEntryLine counterpart = cashLine.getJournalEntry().getLines().stream()
                    .filter(l -> !l.getId().equals(cashLine.getId()))
                    .filter(l -> !CASH_TYPES.contains(l.getAccount().getAccountType()))
                    .findFirst()
                    .orElse(null);
            if (counterpart == null) {
                continue; // pure transfer between our own cash accounts — nets to zero across the pool
            }
            AccountType counterType = counterpart.getAccount().getAccountType();
            Map<String, BigDecimal> bucket = isInvestingType(counterType) ? investing
                    : isFinancingType(counterType) ? financing
                    : operating;
            String label = counterpart.getAccount().getName();
            bucket.merge(label, cashLine.signedAmount(), BigDecimal::add);
        }
        return new CategorizedCashActivity(operating, investing, financing);
    }

    private boolean isInvestingType(AccountType type) {
        return type == AccountType.FIXED_ASSET || type == AccountType.ACCUMULATED_DEPRECIATION;
    }

    private boolean isFinancingType(AccountType type) {
        return type == AccountType.EQUITY || type == AccountType.LONG_TERM_LIABILITY;
    }

    private List<CashFlowLine> buildIndirectOperatingLines(UUID organizationId, LocalDate fromDate, LocalDate toDate) {
        IncomeStatement incomeStatement = getIncomeStatement(fromDate, toDate);
        List<CashFlowLine> lines = new ArrayList<>();
        lines.add(new CashFlowLine("Net Income", incomeStatement.netIncome()));

        Map<UUID, AccountTotalsByAccount> startTotals = totalsAsOfByAccount(organizationId, fromDate.minusDays(1));
        Map<UUID, AccountTotalsByAccount> endTotals = totalsAsOfByAccount(organizationId, toDate);
        List<Account> accounts = accountRepository.findByOrganizationIdOrderByCodeAsc(organizationId);

        for (Account account : accounts) {
            AccountType type = account.getAccountType();
            if (!ASSET_SIDE_WORKING_CAPITAL.contains(type) && !LIABILITY_SIDE_WORKING_CAPITAL.contains(type)) {
                continue;
            }
            BigDecimal startBalance = signedBalance(account, startTotals.get(account.getId()));
            BigDecimal endBalance = signedBalance(account, endTotals.get(account.getId()));
            BigDecimal change = endBalance.subtract(startBalance);
            if (change.signum() == 0) {
                continue;
            }
            if (ASSET_SIDE_WORKING_CAPITAL.contains(type)) {
                lines.add(new CashFlowLine(
                        (change.signum() > 0 ? "Increase" : "Decrease") + " in " + account.getName(), change.negate()));
            } else {
                lines.add(new CashFlowLine(
                        (change.signum() > 0 ? "Increase" : "Decrease") + " in " + account.getName(), change));
            }
        }
        return lines;
    }

    private List<CashFlowLine> toLines(Map<String, BigDecimal> bucket) {
        return bucket.entrySet().stream()
                .map(e -> new CashFlowLine(e.getKey(), e.getValue()))
                .toList();
    }

    private BigDecimal sumValues(Map<String, BigDecimal> bucket) {
        return bucket.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
