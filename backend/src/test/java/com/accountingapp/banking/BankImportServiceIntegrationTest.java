package com.accountingapp.banking;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.Account;
import com.accountingapp.banking.BankImportService.FileFormat;
import com.accountingapp.banking.BankImportService.ImportResult;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.organization.Organization;
import com.accountingapp.support.AbstractIntegrationTest;
import com.accountingapp.support.ArTestFixture;
import com.accountingapp.support.OrganizationTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BankImportServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BankImportService bankImportService;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ArTestFixture arTestFixture;

    private Account bankAccount;

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        bankAccount = arTestFixture.createBankAccount(organization);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void importsCsvTransactions() {
        String csv = """
                Date,Description,Amount
                2026-01-15,Coffee Shop,-4.50
                2026-01-16,Payroll Deposit,2500.00
                """;

        ImportResult result = bankImportService.importFeed(bankAccount.getId(), FileFormat.CSV, csv);

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skippedDuplicates()).isEqualTo(0);
        assertThat(bankTransactionRepository.findByAccountIdOrderByTransactionDateDesc(bankAccount.getId())).hasSize(2);
    }

    @Test
    void reimportingTheSameFileSkipsDuplicates() {
        String csv = """
                Date,Description,Amount
                2026-01-15,Coffee Shop,-4.50
                """;

        bankImportService.importFeed(bankAccount.getId(), FileFormat.CSV, csv);
        ImportResult second = bankImportService.importFeed(bankAccount.getId(), FileFormat.CSV, csv);

        assertThat(second.imported()).isEqualTo(0);
        assertThat(second.skippedDuplicates()).isEqualTo(1);
        assertThat(bankTransactionRepository.findByAccountIdOrderByTransactionDateDesc(bankAccount.getId())).hasSize(1);
    }
}
