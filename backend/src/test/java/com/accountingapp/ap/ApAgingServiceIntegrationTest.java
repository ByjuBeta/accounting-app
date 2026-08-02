package com.accountingapp.ap;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.Account;
import com.accountingapp.ap.dto.ApAgingReport;
import com.accountingapp.ap.dto.BillLineRequest;
import com.accountingapp.ap.dto.CreateBillRequest;
import com.accountingapp.common.context.OrganizationContext;
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

class ApAgingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BillService billService;

    @Autowired
    private ApAgingService apAgingService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    @Autowired
    private ApTestFixture apTestFixture;

    private Vendor vendor;
    private Account expenseAccount;
    private final LocalDate asOfDate = LocalDate.of(2026, 6, 30);

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        OrganizationContext.set(organization.getId());
        vendor = apTestFixture.createVendor(organization);
        expenseAccount = apTestFixture.createExpenseAccount(organization);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    void bucketsOpenBillsByDaysPastDue() {
        receiveBillDueOn(asOfDate.plusDays(5), "100.00");
        receiveBillDueOn(asOfDate.minusDays(15), "200.00");
        receiveBillDueOn(asOfDate.minusDays(45), "300.00");
        receiveBillDueOn(asOfDate.minusDays(75), "400.00");
        receiveBillDueOn(asOfDate.minusDays(120), "500.00");

        ApAgingReport report = apAgingService.getAgingReport(asOfDate);

        assertThat(report.rows()).hasSize(1);
        var bucket = report.rows().get(0).bucket();
        assertThat(bucket.current()).isEqualByComparingTo("100.00");
        assertThat(bucket.days1to30()).isEqualByComparingTo("200.00");
        assertThat(bucket.days31to60()).isEqualByComparingTo("300.00");
        assertThat(bucket.days61to90()).isEqualByComparingTo("400.00");
        assertThat(bucket.over90()).isEqualByComparingTo("500.00");
        assertThat(report.totals().total()).isEqualByComparingTo("1500.00");
    }

    private void receiveBillDueOn(LocalDate dueDate, String amount) {
        BillLineRequest line = new BillLineRequest(
                "Item", BigDecimal.ONE, new BigDecimal(amount), BigDecimal.ZERO, expenseAccount.getId());
        Bill bill = billService.create(new CreateBillRequest(
                vendor.getId(), null, dueDate.minusDays(30), dueDate, null, "USD", List.of(line)));
        billService.receive(bill.getId());
    }
}
