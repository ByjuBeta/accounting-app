package com.accountingapp.ar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.accountingapp.account.Account;
import com.accountingapp.ar.dto.CreateInvoiceRequest;
import com.accountingapp.ar.dto.InvoiceLineRequest;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.journal.JournalEntry;
import com.accountingapp.journal.JournalEntryRepository;
import com.accountingapp.journal.TransactionStatus;
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

class InvoiceServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ArTestFixture arTestFixture;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Organization organization;
    private Customer customer;
    private Account incomeAccount;

    @BeforeEach
    void setUp() {
        organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        customer = arTestFixture.createCustomer(organization);
        incomeAccount = arTestFixture.createIncomeAccount(organization);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void createsDraftInvoiceWithoutPostingToLedger() {
        Invoice invoice = invoiceService.create(twoLineRequest());

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("300.00");
        assertThat(invoice.getTaxTotal()).isEqualByComparingTo("24.00");
        assertThat(invoice.getTotal()).isEqualByComparingTo("324.00");
        assertThat(journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "INVOICE", invoice.getId(), organization.getId())).isEmpty();
    }

    @Test
    @Transactional
    void sendingInvoicePostsBalancedJournalEntry() {
        Invoice invoice = invoiceService.create(twoLineRequest());

        Invoice sent = invoiceService.send(invoice.getId());

        assertThat(sent.getStatus()).isEqualTo(InvoiceStatus.SENT);
        JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "INVOICE", invoice.getId(), organization.getId()).orElseThrow();
        assertThat(entry.getStatus()).isEqualTo(TransactionStatus.POSTED);
        BigDecimal totalDebit = entry.getLines().stream().map(l -> l.getDebitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream().map(l -> l.getCreditAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);
        assertThat(totalDebit).isEqualByComparingTo("324.00");
    }

    @Test
    void cancellingSentInvoiceReversesLedgerEntry() {
        Invoice invoice = invoiceService.create(twoLineRequest());
        invoiceService.send(invoice.getId());

        Invoice cancelled = invoiceService.cancel(invoice.getId());

        assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        JournalEntry original = journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                "INVOICE", invoice.getId(), organization.getId()).orElseThrow();
        assertThat(original.getStatus()).isEqualTo(TransactionStatus.POSTED);
    }

    @Test
    void cannotCancelInvoiceWithPaymentsApplied() {
        Invoice invoice = invoiceService.create(twoLineRequest());
        invoiceService.send(invoice.getId());
        invoiceService.applyPayment(invoice.getId(), new BigDecimal("324.00"));

        assertThatThrownBy(() -> invoiceService.cancel(invoice.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void applyingFullPaymentMarksInvoicePaid() {
        Invoice invoice = invoiceService.create(twoLineRequest());
        invoiceService.send(invoice.getId());

        invoiceService.applyPayment(invoice.getId(), new BigDecimal("324.00"));

        Invoice reloaded = invoiceService.getOrThrow(invoice.getId());
        assertThat(reloaded.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(reloaded.balanceDue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void applyingPartialPaymentMarksInvoicePartiallyPaid() {
        Invoice invoice = invoiceService.create(twoLineRequest());
        invoiceService.send(invoice.getId());

        invoiceService.applyPayment(invoice.getId(), new BigDecimal("100.00"));

        Invoice reloaded = invoiceService.getOrThrow(invoice.getId());
        assertThat(reloaded.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(reloaded.balanceDue()).isEqualByComparingTo("224.00");
    }

    @Test
    void rejectsPaymentExceedingBalanceDue() {
        Invoice invoice = invoiceService.create(twoLineRequest());
        invoiceService.send(invoice.getId());

        assertThatThrownBy(() -> invoiceService.applyPayment(invoice.getId(), new BigDecimal("1000.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    private CreateInvoiceRequest twoLineRequest() {
        InvoiceLineRequest line1 = new InvoiceLineRequest(
                "Consulting", new BigDecimal("2"), new BigDecimal("100.00"), new BigDecimal("8.00"), incomeAccount.getId());
        InvoiceLineRequest line2 = new InvoiceLineRequest(
                "Support", new BigDecimal("1"), new BigDecimal("100.00"), new BigDecimal("8.00"), incomeAccount.getId());
        return new CreateInvoiceRequest(customer.getId(), LocalDate.now(), null, "Test invoice", "USD",
                List.of(line1, line2));
    }
}
