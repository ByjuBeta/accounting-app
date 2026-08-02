package com.accountingapp.ap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.accountingapp.account.Account;
import com.accountingapp.ap.dto.BillLineRequest;
import com.accountingapp.ap.dto.CreateBillRequest;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.journal.JournalEntry;
import com.accountingapp.journal.JournalEntryRepository;
import com.accountingapp.journal.TransactionStatus;
import com.accountingapp.organization.Organization;
import com.accountingapp.support.AbstractIntegrationTest;
import com.accountingapp.support.ApTestFixture;
import com.accountingapp.support.OrganizationTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class BillServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BillService billService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ApTestFixture apTestFixture;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Organization organization;
    private Vendor vendor;
    private Account expenseAccount;

    @BeforeEach
    void setUp() {
        organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        vendor = apTestFixture.createVendor(organization);
        expenseAccount = apTestFixture.createExpenseAccount(organization);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void createsDraftBillWithoutPostingToLedger() {
        Bill bill = billService.create(twoLineRequest());

        assertThat(bill.getStatus()).isEqualTo(BillStatus.DRAFT);
        assertThat(bill.getSubtotal()).isEqualByComparingTo("300.00");
        assertThat(bill.getTaxTotal()).isEqualByComparingTo("24.00");
        assertThat(bill.getTotal()).isEqualByComparingTo("324.00");
        assertThat(journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "BILL", bill.getId(), organization.getId())).isEmpty();
    }

    @Test
    @Transactional
    void receivingBillPostsBalancedJournalEntry() {
        Bill bill = billService.create(twoLineRequest());

        Bill received = billService.receive(bill.getId());

        assertThat(received.getStatus()).isEqualTo(BillStatus.RECEIVED);
        JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "BILL", bill.getId(), organization.getId()).orElseThrow();
        assertThat(entry.getStatus()).isEqualTo(TransactionStatus.POSTED);
        BigDecimal totalDebit = entry.getLines().stream().map(l -> l.getDebitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream().map(l -> l.getCreditAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit).isEqualByComparingTo("324.00");
    }

    @Test
    void cancellingReceivedBillReversesLedgerEntry() {
        Bill bill = billService.create(twoLineRequest());
        billService.receive(bill.getId());

        Bill cancelled = billService.cancel(bill.getId());

        assertThat(cancelled.getStatus()).isEqualTo(BillStatus.CANCELLED);
    }

    @Test
    void cannotCancelBillWithPaymentsApplied() {
        Bill bill = billService.create(twoLineRequest());
        billService.receive(bill.getId());
        billService.applyPayment(bill.getId(), new BigDecimal("324.00"));

        assertThatThrownBy(() -> billService.cancel(bill.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void applyingFullPaymentMarksBillPaid() {
        Bill bill = billService.create(twoLineRequest());
        billService.receive(bill.getId());

        billService.applyPayment(bill.getId(), new BigDecimal("324.00"));

        Bill reloaded = billService.getOrThrow(bill.getId());
        assertThat(reloaded.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(reloaded.balanceDue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsPaymentExceedingBalanceDue() {
        Bill bill = billService.create(twoLineRequest());
        billService.receive(bill.getId());

        assertThatThrownBy(() -> billService.applyPayment(bill.getId(), new BigDecimal("1000.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsNonExpenseAccountForLine() {
        Account bankAccount = apTestFixture.createBankAccount(organization);
        BillLineRequest badLine = new BillLineRequest(
                "Bad", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO, bankAccount.getId());
        CreateBillRequest request = new CreateBillRequest(
                vendor.getId(), null, LocalDate.now(), null, null, "USD", List.of(badLine));

        assertThatThrownBy(() -> billService.create(request)).isInstanceOf(BusinessRuleException.class);
    }

    private CreateBillRequest twoLineRequest() {
        BillLineRequest line1 = new BillLineRequest(
                "Supplies", new BigDecimal("2"), new BigDecimal("100.00"), new BigDecimal("8.00"), expenseAccount.getId());
        BillLineRequest line2 = new BillLineRequest(
                "Services", new BigDecimal("1"), new BigDecimal("100.00"), new BigDecimal("8.00"), expenseAccount.getId());
        return new CreateBillRequest(vendor.getId(), "VREF-1", LocalDate.now(), null, "Test bill", "USD",
                List.of(line1, line2));
    }
}
