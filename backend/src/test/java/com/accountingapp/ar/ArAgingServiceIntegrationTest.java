package com.accountingapp.ar;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.Account;
import com.accountingapp.ar.dto.ArAgingReport;
import com.accountingapp.ar.dto.CreateInvoiceRequest;
import com.accountingapp.ar.dto.InvoiceLineRequest;
import com.accountingapp.common.context.OrganizationContext;
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

class ArAgingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ArAgingService arAgingService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ArTestFixture arTestFixture;

    private Customer customer;
    private Account incomeAccount;
    private final LocalDate asOfDate = LocalDate.of(2026, 6, 30);

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        customer = arTestFixture.createCustomer(organization);
        incomeAccount = arTestFixture.createIncomeAccount(organization);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void bucketsOpenInvoicesByDaysPastDue() {
        sendInvoiceDueOn(asOfDate.plusDays(5), "100.00");    // not yet due -> current
        sendInvoiceDueOn(asOfDate.minusDays(15), "200.00");  // 1-30
        sendInvoiceDueOn(asOfDate.minusDays(45), "300.00");  // 31-60
        sendInvoiceDueOn(asOfDate.minusDays(75), "400.00");  // 61-90
        sendInvoiceDueOn(asOfDate.minusDays(120), "500.00"); // 90+

        ArAgingReport report = arAgingService.getAgingReport(asOfDate);

        assertThat(report.rows()).hasSize(1);
        var bucket = report.rows().get(0).bucket();
        assertThat(bucket.current()).isEqualByComparingTo("100.00");
        assertThat(bucket.days1to30()).isEqualByComparingTo("200.00");
        assertThat(bucket.days31to60()).isEqualByComparingTo("300.00");
        assertThat(bucket.days61to90()).isEqualByComparingTo("400.00");
        assertThat(bucket.over90()).isEqualByComparingTo("500.00");
        assertThat(report.totals().total()).isEqualByComparingTo("1500.00");
    }

    private void sendInvoiceDueOn(LocalDate dueDate, String amount) {
        InvoiceLineRequest line = new InvoiceLineRequest(
                "Item", BigDecimal.ONE, new BigDecimal(amount), BigDecimal.ZERO, incomeAccount.getId());
        Invoice invoice = invoiceService.create(new CreateInvoiceRequest(
                customer.getId(), dueDate.minusDays(30), dueDate, null, "USD", List.of(line)));
        invoiceService.send(invoice.getId());
    }
}
