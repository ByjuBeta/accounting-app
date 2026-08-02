package com.accountingapp.reports;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountType;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.journal.JournalEntry;
import com.accountingapp.journal.JournalEntryService;
import com.accountingapp.journal.TransactionType;
import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryLineRequest;
import com.accountingapp.organization.Organization;
import com.accountingapp.reports.dto.BalanceSheet;
import com.accountingapp.support.AbstractIntegrationTest;
import com.accountingapp.support.OrganizationTestFixture;
import com.accountingapp.support.ReportsTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BalanceSheetIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReportsService reportsService;

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ReportsTestFixture reportsTestFixture;

    private Account bank;
    private Account equity;
    private Account revenue;
    private Account expense;

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        bank = reportsTestFixture.create(organization, "1000", "Checking", AccountType.BANK);
        equity = reportsTestFixture.create(organization, "3000", "Owner's Equity", AccountType.EQUITY);
        revenue = reportsTestFixture.create(organization, "4000", "Sales Revenue", AccountType.INCOME);
        expense = reportsTestFixture.create(organization, "6000", "Office Supplies", AccountType.EXPENSE);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void assetsEqualLiabilitiesPlusEquityIncludingCurrentNetIncome() {
        post(LocalDate.of(2026, 1, 1), bank, equity, new BigDecimal("10000.00")); // owner contribution
        post(LocalDate.of(2026, 1, 10), bank, revenue, new BigDecimal("2000.00")); // cash sale
        post(LocalDate.of(2026, 1, 15), expense, bank, new BigDecimal("300.00")); // paid an expense

        BalanceSheet balanceSheet = reportsService.getBalanceSheet(LocalDate.of(2026, 1, 31));

        assertThat(balanceSheet.balanced()).isTrue();
        assertThat(balanceSheet.netIncomeToDate()).isEqualByComparingTo("1700.00"); // 2000 revenue - 300 expense
        assertThat(balanceSheet.assets().total()).isEqualByComparingTo("11700.00"); // 10000 + 2000 - 300
        assertThat(balanceSheet.equity().total()).isEqualByComparingTo("11700.00"); // 10000 contribution + 1700 net income
    }

    private void post(LocalDate date, Account debitAccount, Account creditAccount, BigDecimal amount) {
        JournalEntryLineRequest debit = new JournalEntryLineRequest(debitAccount.getId(), amount, BigDecimal.ZERO, null, Set.of());
        JournalEntryLineRequest credit = new JournalEntryLineRequest(creditAccount.getId(), BigDecimal.ZERO, amount, null, Set.of());
        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                date, TransactionType.JOURNAL_ENTRY, "Test", null, "USD", List.of(debit, credit));
        JournalEntry entry = journalEntryService.create(request);
        journalEntryService.post(entry.getId());
    }
}
