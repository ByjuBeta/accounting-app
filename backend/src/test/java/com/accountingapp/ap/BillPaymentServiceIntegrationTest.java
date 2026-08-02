package com.accountingapp.ap;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.Account;
import com.accountingapp.ap.dto.BillLineRequest;
import com.accountingapp.ap.dto.BillPaymentApplicationRequest;
import com.accountingapp.ap.dto.CreateBillPaymentRequest;
import com.accountingapp.ap.dto.CreateBillRequest;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.journal.JournalEntry;
import com.accountingapp.journal.JournalEntryRepository;
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

class BillPaymentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BillService billService;

    @Autowired
    private BillPaymentService billPaymentService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ApTestFixture apTestFixture;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Organization organization;
    private Account bankAccount;
    private Account expenseAccount;

    @BeforeEach
    void setUp() {
        organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        expenseAccount = apTestFixture.createExpenseAccount(organization);
        bankAccount = apTestFixture.createBankAccount(organization);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    @Transactional
    void exactPaymentMarksBillPaidAndBalances() {
        Vendor vendor = apTestFixture.createVendor(organization);
        Bill bill = createAndReceiveBill(vendor, "500.00");

        BillPayment payment = billPaymentService.create(new CreateBillPaymentRequest(
                vendor.getId(), LocalDate.now(), new BigDecimal("500.00"), bankAccount.getId(), null, null,
                List.of(new BillPaymentApplicationRequest(bill.getId(), new BigDecimal("500.00"), null))));

        assertThat(payment.unappliedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(billService.getOrThrow(bill.getId()).getStatus()).isEqualTo(BillStatus.PAID);

        JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "BILL_PAYMENT", payment.getId(), organization.getId()).orElseThrow();
        BigDecimal totalDebit = entry.getLines().stream().map(l -> l.getDebitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream().map(l -> l.getCreditAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit).isEqualByComparingTo("500.00");
    }

    @Test
    @Transactional
    void earlyPaymentDiscountReducesCashPaidButFullyClearsBill() {
        Vendor vendor = apTestFixture.createVendor(organization, new BigDecimal("2.00"), 10);
        Bill bill = createAndReceiveBill(vendor, "1000.00");
        assertThat(bill.isEligibleForEarlyPaymentDiscount(bill.getBillDate().plusDays(5))).isTrue();
        assertThat(bill.earlyPaymentDiscountAmount()).isEqualByComparingTo("20.00");

        BillPayment payment = billPaymentService.create(new CreateBillPaymentRequest(
                vendor.getId(), bill.getBillDate().plusDays(5), new BigDecimal("980.00"), bankAccount.getId(), null, null,
                List.of(new BillPaymentApplicationRequest(bill.getId(), new BigDecimal("980.00"), new BigDecimal("20.00")))));

        assertThat(billService.getOrThrow(bill.getId()).getStatus()).isEqualTo(BillStatus.PAID);

        JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "BILL_PAYMENT", payment.getId(), organization.getId()).orElseThrow();
        BigDecimal totalDebit = entry.getLines().stream().map(l -> l.getDebitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream().map(l -> l.getCreditAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit).isEqualByComparingTo("1000.00");
        assertThat(entry.getLines()).hasSize(3);
    }

    @Test
    @Transactional
    void prepaymentLeavesVendorCreditAndStillBalances() {
        Vendor vendor = apTestFixture.createVendor(organization);
        Bill bill = createAndReceiveBill(vendor, "500.00");

        BillPayment payment = billPaymentService.create(new CreateBillPaymentRequest(
                vendor.getId(), LocalDate.now(), new BigDecimal("600.00"), bankAccount.getId(), null, null,
                List.of(new BillPaymentApplicationRequest(bill.getId(), new BigDecimal("500.00"), null))));

        assertThat(payment.unappliedAmount()).isEqualByComparingTo("100.00");
        JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "BILL_PAYMENT", payment.getId(), organization.getId()).orElseThrow();
        BigDecimal totalDebit = entry.getLines().stream().map(l -> l.getDebitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream().map(l -> l.getCreditAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit).isEqualByComparingTo("600.00");
    }

    private Bill createAndReceiveBill(Vendor vendor, String amount) {
        BillLineRequest line = new BillLineRequest(
                "Supplies", BigDecimal.ONE, new BigDecimal(amount), BigDecimal.ZERO, expenseAccount.getId());
        Bill bill = billService.create(new CreateBillRequest(
                vendor.getId(), null, LocalDate.now(), null, null, "USD", List.of(line)));
        return billService.receive(bill.getId());
    }
}
