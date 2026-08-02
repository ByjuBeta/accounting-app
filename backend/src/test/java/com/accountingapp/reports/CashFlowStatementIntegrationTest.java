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
import com.accountingapp.reports.dto.CashFlowMethod;
import com.accountingapp.reports.dto.CashFlowStatement;
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

class CashFlowStatementIntegrationTest extends AbstractIntegrationTest {

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
    private Account fixedAsset;

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        bank = reportsTestFixture.create(organization, "1000", "Checking", AccountType.BANK);
        equity = reportsTestFixture.create(organization, "3000", "Owner's Equity", AccountType.EQUITY);
        revenue = reportsTestFixture.create(organization, "4000", "Sales Revenue", AccountType.INCOME);
        expense = reportsTestFixture.create(organization, "6000", "Office Supplies", AccountType.EXPENSE);
        fixedAsset = reportsTestFixture.create(organization, "1500", "Equipment", AccountType.FIXED_ASSET);

        post(LocalDate.of(2026, 1, 1), bank, equity, new BigDecimal("10000.00"));   // financing
        post(LocalDate.of(2026, 1, 10), bank, revenue, new BigDecimal("2000.00"));  // operating in
        post(LocalDate.of(2026, 1, 15), fixedAsset, bank, new BigDecimal("1500.00")); // investing out
        post(LocalDate.of(2026, 1, 20), expense, bank, new BigDecimal("300.00"));   // operating out
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void directMethodCategorizesByCounterpartyAndSumsToNetChangeInCash() {
        CashFlowStatement statement = reportsService.getCashFlowStatement(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), CashFlowMethod.DIRECT);

        assertThat(statement.operatingTotal()).isEqualByComparingTo("1700.00");
        assertThat(statement.investingTotal()).isEqualByComparingTo("-1500.00");
        assertThat(statement.financingTotal()).isEqualByComparingTo("10000.00");
        assertThat(statement.beginningCash()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(statement.endingCash()).isEqualByComparingTo("10200.00");
        assertThat(statement.netChangeInCash()).isEqualByComparingTo("10200.00");
        assertThat(statement.operatingTotal().add(statement.investingTotal()).add(statement.financingTotal()))
                .isEqualByComparingTo(statement.netChangeInCash());
    }

    @Test
    void indirectMethodOperatingMatchesDirectWhenNoWorkingCapitalMovement() {
        CashFlowStatement statement = reportsService.getCashFlowStatement(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), CashFlowMethod.INDIRECT);

        assertThat(statement.operatingTotal()).isEqualByComparingTo("1700.00");
        assertThat(statement.investingTotal()).isEqualByComparingTo("-1500.00");
        assertThat(statement.financingTotal()).isEqualByComparingTo("10000.00");
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
