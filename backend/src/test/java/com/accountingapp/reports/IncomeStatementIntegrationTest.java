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
import com.accountingapp.reports.dto.IncomeStatement;
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

class IncomeStatementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReportsService reportsService;

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ReportsTestFixture reportsTestFixture;

    private Account bank;
    private Account revenue;
    private Account expense;

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        bank = reportsTestFixture.create(organization, "1000", "Checking", AccountType.BANK);
        revenue = reportsTestFixture.create(organization, "4000", "Sales Revenue", AccountType.INCOME);
        expense = reportsTestFixture.create(organization, "6000", "Office Supplies", AccountType.EXPENSE);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void onlyIncludesActivityWithinThePeriod() {
        post(LocalDate.of(2026, 1, 15), bank, revenue, new BigDecimal("1000.00")); // in period
        post(LocalDate.of(2026, 1, 20), expense, bank, new BigDecimal("400.00")); // in period
        post(LocalDate.of(2026, 2, 5), bank, revenue, new BigDecimal("5000.00")); // out of period

        IncomeStatement statement = reportsService.getIncomeStatement(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(statement.totalRevenue()).isEqualByComparingTo("1000.00");
        assertThat(statement.totalExpense()).isEqualByComparingTo("400.00");
        assertThat(statement.netIncome()).isEqualByComparingTo("600.00");
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
