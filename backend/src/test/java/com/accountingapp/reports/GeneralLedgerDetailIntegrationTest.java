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
import com.accountingapp.reports.dto.GeneralLedgerDetail;
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

class GeneralLedgerDetailIntegrationTest extends AbstractIntegrationTest {

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

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        bank = reportsTestFixture.create(organization, "1000", "Checking", AccountType.BANK);
        revenue = reportsTestFixture.create(organization, "4000", "Sales Revenue", AccountType.INCOME);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void runningBalanceAccumulatesInAccountsNormalBalanceDirection() {
        post(LocalDate.of(2025, 12, 20), bank, revenue, new BigDecimal("500.00")); // before period, sets beginning balance
        post(LocalDate.of(2026, 1, 5), bank, revenue, new BigDecimal("300.00"));
        post(LocalDate.of(2026, 1, 10), revenue, bank, new BigDecimal("100.00")); // a withdrawal against the bank

        GeneralLedgerDetail detail = reportsService.getGeneralLedgerDetail(
                bank.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(detail.beginningBalance()).isEqualByComparingTo("500.00");
        assertThat(detail.lines()).hasSize(2);
        assertThat(detail.lines().get(0).runningBalance()).isEqualByComparingTo("800.00");
        assertThat(detail.lines().get(1).runningBalance()).isEqualByComparingTo("700.00");
        assertThat(detail.endingBalance()).isEqualByComparingTo("700.00");
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
