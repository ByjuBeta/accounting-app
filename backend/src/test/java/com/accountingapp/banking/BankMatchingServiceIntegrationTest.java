package com.accountingapp.banking;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.Account;
import com.accountingapp.common.context.OrganizationContext;
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

class BankMatchingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BankImportService bankImportService;

    @Autowired
    private BankMatchingService bankMatchingService;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

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
    void autoMatchesUnambiguousCandidate() {
        postDeposit(LocalDate.of(2026, 1, 16), "2500.00");
        bankImportService.importFeed(bankAccount.getId(), BankImportService.FileFormat.CSV, """
                Date,Description,Amount
                2026-01-16,Payroll Deposit,2500.00
                """);

        int matched = bankMatchingService.autoMatch(bankAccount.getId());

        assertThat(matched).isEqualTo(1);
        BankTransaction transaction = bankTransactionRepository.findByAccountIdOrderByTransactionDateDesc(bankAccount.getId()).get(0);
        assertThat(transaction.getStatus()).isEqualTo(BankTransactionStatus.MATCHED);
        assertThat(transaction.getMatchedJournalEntryLine()).isNotNull();
    }

    @Test
    void doesNotAutoMatchWhenMultipleCandidatesExist() {
        postDeposit(LocalDate.of(2026, 1, 16), "500.00");
        postDeposit(LocalDate.of(2026, 1, 17), "500.00");
        bankImportService.importFeed(bankAccount.getId(), BankImportService.FileFormat.CSV, """
                Date,Description,Amount
                2026-01-16,Deposit,500.00
                """);

        int matched = bankMatchingService.autoMatch(bankAccount.getId());

        assertThat(matched).isEqualTo(0);
        BankTransaction transaction = bankTransactionRepository.findByAccountIdOrderByTransactionDateDesc(bankAccount.getId()).get(0);
        assertThat(transaction.getStatus()).isEqualTo(BankTransactionStatus.UNMATCHED);
        assertThat(bankMatchingService.suggestMatches(transaction.getId())).hasSize(2);
    }

    @Test
    void ignoringATransactionRemovesItFromUnmatchedList() {
        bankImportService.importFeed(bankAccount.getId(), BankImportService.FileFormat.CSV, """
                Date,Description,Amount
                2026-01-16,Bank Fee,-15.00
                """);
        BankTransaction transaction = bankTransactionRepository.findByAccountIdOrderByTransactionDateDesc(bankAccount.getId()).get(0);

        bankMatchingService.ignore(transaction.getId());

        assertThat(bankTransactionRepository.findByAccountIdAndStatusOrderByTransactionDateAsc(
                bankAccount.getId(), BankTransactionStatus.UNMATCHED)).isEmpty();
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
