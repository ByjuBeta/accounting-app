package com.accountingapp.ar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.accountingapp.account.Account;
import com.accountingapp.ar.dto.CreateInvoiceRequest;
import com.accountingapp.ar.dto.CreatePaymentRequest;
import com.accountingapp.ar.dto.InvoiceLineRequest;
import com.accountingapp.ar.dto.PaymentApplicationRequest;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.journal.JournalEntry;
import com.accountingapp.journal.JournalEntryRepository;
import com.accountingapp.organization.Organization;
import com.accountingapp.support.AbstractIntegrationTest;
import com.accountingapp.support.ArTestFixture;
import com.accountingapp.support.OrganizationTestFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class PaymentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ArTestFixture arTestFixture;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Organization organization;
    private Customer customer;
    private Account bankAccount;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        customer = arTestFixture.createCustomer(organization);
        Account incomeAccount = arTestFixture.createIncomeAccount(organization);
        bankAccount = arTestFixture.createBankAccount(organization);

        InvoiceLineRequest line = new InvoiceLineRequest(
                "Consulting", BigDecimal.ONE, new BigDecimal("500.00"), BigDecimal.ZERO, incomeAccount.getId());
        invoice = invoiceService.create(new CreateInvoiceRequest(
                customer.getId(), LocalDate.now(), null, null, "USD", List.of(line)));
        invoiceService.send(invoice.getId());
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    @Transactional
    void exactPaymentMarksInvoicePaidWithNoUnappliedAmount() {
        Payment payment = paymentService.create(new CreatePaymentRequest(
                customer.getId(), LocalDate.now(), new BigDecimal("500.00"), bankAccount.getId(), null, null,
                List.of(new PaymentApplicationRequest(invoice.getId(), new BigDecimal("500.00")))));

        assertThat(payment.unappliedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoiceService.getOrThrow(invoice.getId()).getStatus()).isEqualTo(InvoiceStatus.PAID);

        JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "PAYMENT", payment.getId(), organization.getId()).orElseThrow();
        BigDecimal totalDebit = entry.getLines().stream().map(l -> l.getDebitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream().map(l -> l.getCreditAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit).isEqualByComparingTo("500.00");
    }

    @Test
    @Transactional
    void overpaymentLeavesUnappliedCreditAndStillBalances() {
        Payment payment = paymentService.create(new CreatePaymentRequest(
                customer.getId(), LocalDate.now(), new BigDecimal("600.00"), bankAccount.getId(), null, null,
                List.of(new PaymentApplicationRequest(invoice.getId(), new BigDecimal("500.00")))));

        assertThat(payment.unappliedAmount()).isEqualByComparingTo("100.00");
        assertThat(invoiceService.getOrThrow(invoice.getId()).getStatus()).isEqualTo(InvoiceStatus.PAID);

        JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "PAYMENT", payment.getId(), organization.getId()).orElseThrow();
        BigDecimal totalDebit = entry.getLines().stream().map(l -> l.getDebitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream().map(l -> l.getCreditAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit).isEqualByComparingTo("600.00");
        assertThat(entry.getLines()).hasSize(3);
    }

    @Test
    void rejectsApplicationsExceedingPaymentAmount() {
        assertThatThrownBy(() -> paymentService.create(new CreatePaymentRequest(
                customer.getId(), LocalDate.now(), new BigDecimal("100.00"), bankAccount.getId(), null, null,
                List.of(new PaymentApplicationRequest(invoice.getId(), new BigDecimal("500.00"))))))
                .isInstanceOf(BusinessRuleException.class);
    }
}
