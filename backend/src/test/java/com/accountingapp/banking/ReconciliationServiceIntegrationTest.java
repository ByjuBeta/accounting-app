package com.accountingapp.banking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.accountingapp.account.Account;
import com.accountingapp.banking.dto.ReconciliationWorksheet;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.journal.JournalEntry;
import com.accountingapp.journal.JournalEntryService;
import com.accountingapp.journal.TransactionType;
import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryLineRequest;
import com.accountingapp.organization.Organization;
import com.accountingapp.support.AbstractIntegrationTest;
import com.accountingapp.support.ArTestFixture;
import com.accountingapp.support.OrganizationTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReconciliationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private BankImportService bankImportService;

    @Autowired
    private BankMatchingService bankMatchingService;

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ArTestFixture arTestFixture;

    private Account bankAccount;
    private Account incomeAccount;

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        bankAccount = arTestFixture.createBankAccount(organization);
        incomeAccount = arTestFixture.createIncomeAccount(organization);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void worksheetReflectsClearedAndOutstandingLines() {
        postDeposit(LocalDate.of(2026, 1, 10), "1000.00"); // will be matched -> cleared
        postDeposit(LocalDate.of(2026, 1, 20), "250.00");  // no matching bank feed row -> outstanding

        bankImportService.importFeed(bankAccount.getId(), BankImportService.FileFormat.CSV, """
                Date,Description,Amount
                2026-01-10,Deposit,1000.00
                """);
        bankMatchingService.autoMatch(bankAccount.getId());

        Reconciliation reconciliation = reconciliationService.start(
                bankAccount.getId(), LocalDate.of(2026, 1, 31), new BigDecimal("1000.00"));
        ReconciliationWorksheet worksheet = reconciliationService.getWorksheet(reconciliation.getId());

        assertThat(worksheet.clearedTotal()).isEqualByComparingTo("1000.00");
        assertThat(worksheet.clearedTransactionCount()).isEqualTo(1);
        assertThat(worksheet.outstandingLines()).hasSize(1);
        assertThat(worksheet.outstandingLines().get(0).amount()).isEqualByComparingTo("250.00");
        assertThat(worksheet.isBalanced()).isTrue();
    }

    @Test
    void cannotCompleteWhenStatementBalanceDoesNotMatchClearedTotal() {
        postDeposit(LocalDate.of(2026, 1, 10), "1000.00");
        bankImportService.importFeed(bankAccount.getId(), BankImportService.FileFormat.CSV, """
                Date,Description,Amount
                2026-01-10,Deposit,1000.00
                """);
        bankMatchingService.autoMatch(bankAccount.getId());

        Reconciliation reconciliation = reconciliationService.start(
                bankAccount.getId(), LocalDate.of(2026, 1, 31), new BigDecimal("999.00"));

        assertThatThrownBy(() -> reconciliationService.complete(reconciliation.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void completingLocksClearedLinesAndSeedsNextBeginningBalance() {
        postDeposit(LocalDate.of(2026, 1, 10), "1000.00");
        bankImportService.importFeed(bankAccount.getId(), BankImportService.FileFormat.CSV, """
                Date,Description,Amount
                2026-01-10,Deposit,1000.00
                """);
        bankMatchingService.autoMatch(bankAccount.getId());

        Reconciliation first = reconciliationService.start(
                bankAccount.getId(), LocalDate.of(2026, 1, 31), new BigDecimal("1000.00"));
        Reconciliation completed = reconciliationService.complete(first.getId());
        assertThat(completed.getStatus()).isEqualTo(ReconciliationStatus.COMPLETED);

        // The cleared line is now reconciled, so a second reconciliation must not see it as outstanding again,
        // and its beginning balance should pick up where the first one's ending balance left off.
        postDeposit(LocalDate.of(2026, 2, 5), "300.00");
        bankImportService.importFeed(bankAccount.getId(), BankImportService.FileFormat.CSV, """
                Date,Description,Amount
                2026-02-05,Deposit,300.00
                """);
        bankMatchingService.autoMatch(bankAccount.getId());

        Reconciliation second = reconciliationService.start(
                bankAccount.getId(), LocalDate.of(2026, 2, 28), new BigDecimal("1300.00"));
        assertThat(second.getBeginningBalance()).isEqualByComparingTo("1000.00");

        ReconciliationWorksheet worksheet = reconciliationService.getWorksheet(second.getId());
        assertThat(worksheet.clearedTotal()).isEqualByComparingTo("300.00");
        assertThat(worksheet.isBalanced()).isTrue();
    }

    private void postDeposit(LocalDate date, String amount) {
        JournalEntryLineRequest debit = new JournalEntryLineRequest(
                bankAccount.getId(), new BigDecimal(amount), BigDecimal.ZERO, "Deposit", Set.of());
        JournalEntryLineRequest credit = new JournalEntryLineRequest(
                incomeAccount.getId(), BigDecimal.ZERO, new BigDecimal(amount), "Deposit", Set.of());
        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                date, TransactionType.DEPOSIT, "Deposit", null, "USD", List.of(debit, credit));
        JournalEntry entry = journalEntryService.create(request);
        journalEntryService.post(entry.getId());
    }
}
