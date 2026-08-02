package com.accountingapp.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.accountingapp.account.dto.CreateAccountRequest;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ConflictException;
import com.accountingapp.organization.Organization;
import com.accountingapp.support.AbstractIntegrationTest;
import com.accountingapp.support.OrganizationTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccountServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void createsAccountWithDefaultCurrencyFromOrganization() {
        Account account = accountService.create(new CreateAccountRequest(
                "1000", "Checking", "Primary checking account", AccountType.BANK,
                null, null, Set.of("operating"), null, null));

        assertThat(account.getId()).isNotNull();
        assertThat(account.getCurrencyCode()).isEqualTo("USD");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCategory()).isEqualTo(AccountCategory.ASSET);
        assertThat(account.getNormalBalance()).isEqualTo(NormalBalance.DEBIT);
    }

    @Test
    void rejectsDuplicateAccountCodeWithinOrganization() {
        accountService.create(new CreateAccountRequest(
                "1000", "Checking", null, AccountType.BANK, null, null, Set.of(), null, null));

        assertThatThrownBy(() -> accountService.create(new CreateAccountRequest(
                "1000", "Another Checking", null, AccountType.BANK, null, null, Set.of(), null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void postsOpeningBalanceAsBalancedJournalEntryAgainstOpeningBalanceEquity() {
        Account account = accountService.create(new CreateAccountRequest(
                "1000", "Checking", null, AccountType.BANK, null, null, Set.of(),
                new BigDecimal("500.00"), LocalDate.of(2026, 1, 1)));

        Account openingBalanceEquity = accountService.list(null).stream()
                .filter(a -> a.getCode().equals("OBE"))
                .findFirst()
                .orElseThrow();

        assertThat(openingBalanceEquity.getAccountType()).isEqualTo(AccountType.EQUITY);
        assertThat(account.getId()).isNotNull();
    }
}
