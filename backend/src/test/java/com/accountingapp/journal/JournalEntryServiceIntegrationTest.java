package com.accountingapp.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountService;
import com.accountingapp.account.AccountType;
import com.accountingapp.account.dto.CreateAccountRequest;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryLineRequest;
import com.accountingapp.organization.Organization;
import com.accountingapp.support.AbstractIntegrationTest;
import com.accountingapp.support.OrganizationTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JournalEntryServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    private Account bank;
    private Account revenue;

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        bank = accountService.create(new CreateAccountRequest(
                "1000", "Checking", null, AccountType.BANK, null, null, Set.of(), null, null));
        revenue = accountService.create(new CreateAccountRequest(
                "4000", "Sales Revenue", null, AccountType.INCOME, null, null, Set.of(), null, null));
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void createsBalancedDraftEntry() {
        JournalEntry entry = journalEntryService.create(balancedRequest(new BigDecimal("100.00")));

        assertThat(entry.getStatus()).isEqualTo(TransactionStatus.DRAFT);
        assertThat(entry.getEntryNumber()).startsWith("JE-");
        assertThat(entry.getLines()).hasSize(2);
    }

    @Test
    void rejectsUnbalancedEntry() {
        JournalEntryLineRequest debitLine = new JournalEntryLineRequest(
                bank.getId(), new BigDecimal("100.00"), BigDecimal.ZERO, null, Set.of());
        JournalEntryLineRequest creditLine = new JournalEntryLineRequest(
                revenue.getId(), BigDecimal.ZERO, new BigDecimal("75.00"), null, Set.of());
        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                LocalDate.now(), TransactionType.JOURNAL_ENTRY, "Unbalanced", null, "USD",
                List.of(debitLine, creditLine));

        assertThatThrownBy(() -> journalEntryService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Debits");
    }

    @Test
    void rejectsLineWithBothDebitAndCredit() {
        JournalEntryLineRequest badLine = new JournalEntryLineRequest(
                bank.getId(), new BigDecimal("100.00"), new BigDecimal("100.00"), null, Set.of());
        JournalEntryLineRequest creditLine = new JournalEntryLineRequest(
                revenue.getId(), BigDecimal.ZERO, new BigDecimal("100.00"), null, Set.of());
        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                LocalDate.now(), TransactionType.JOURNAL_ENTRY, "Bad line", null, "USD",
                List.of(badLine, creditLine));

        assertThatThrownBy(() -> journalEntryService.create(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void postingTransitionsDraftToPosted() {
        JournalEntry entry = journalEntryService.create(balancedRequest(new BigDecimal("100.00")));

        JournalEntry posted = journalEntryService.post(entry.getId());

        assertThat(posted.getStatus()).isEqualTo(TransactionStatus.POSTED);
        assertThat(posted.getPostedAt()).isNotNull();
    }

    @Test
    void cannotPostAnAlreadyPostedEntry() {
        JournalEntry entry = journalEntryService.create(balancedRequest(new BigDecimal("100.00")));
        journalEntryService.post(entry.getId());

        assertThatThrownBy(() -> journalEntryService.post(entry.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reversalSwapsDebitsAndCreditsAndLinksToOriginal() {
        JournalEntry entry = journalEntryService.create(balancedRequest(new BigDecimal("100.00")));
        JournalEntry posted = journalEntryService.post(entry.getId());

        JournalEntry reversal = journalEntryService.reverse(posted.getId(), LocalDate.now());

        assertThat(reversal.getStatus()).isEqualTo(TransactionStatus.POSTED);
        assertThat(reversal.getReversalOf().getId()).isEqualTo(posted.getId());
        JournalEntryLine originalBankLine = posted.getLines().stream()
                .filter(l -> l.getAccount().getId().equals(bank.getId())).findFirst().orElseThrow();
        JournalEntryLine reversedBankLine = reversal.getLines().stream()
                .filter(l -> l.getAccount().getId().equals(bank.getId())).findFirst().orElseThrow();
        assertThat(reversedBankLine.getCreditAmount()).isEqualByComparingTo(originalBankLine.getDebitAmount());
    }

    @Test
    void deletingNonDraftEntryIsRejected() {
        JournalEntry entry = journalEntryService.create(balancedRequest(new BigDecimal("100.00")));
        journalEntryService.post(entry.getId());

        assertThatThrownBy(() -> journalEntryService.deleteDraft(entry.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    private CreateJournalEntryRequest balancedRequest(BigDecimal amount) {
        JournalEntryLineRequest debitLine = new JournalEntryLineRequest(
                bank.getId(), amount, BigDecimal.ZERO, "Deposit", Set.of());
        JournalEntryLineRequest creditLine = new JournalEntryLineRequest(
                revenue.getId(), BigDecimal.ZERO, amount, "Sale", Set.of());
        return new CreateJournalEntryRequest(
                LocalDate.now(), TransactionType.DEPOSIT, "Test entry", null, "USD",
                List.of(debitLine, creditLine));
    }
}
