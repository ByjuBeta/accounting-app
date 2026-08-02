package com.accountingapp.journal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountService;
import com.accountingapp.account.AccountType;
import com.accountingapp.account.dto.CreateAccountRequest;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.organization.Organization;
import com.accountingapp.support.AbstractIntegrationTest;
import com.accountingapp.support.OrganizationTestFixture;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves the balance constraint holds even when application-level validation
 * is bypassed entirely — plain JDBC inserts straight into
 * {@code journal_entry_lines}, no {@code JournalEntryService} involved.
 */
class JournalBalanceDatabaseTriggerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OrganizationTestFixture organizationTestFixture;

    private UUID organizationId;
    private Account bank;
    private Account revenue;

    @BeforeEach
    void setUp() {
        Organization organization = organizationTestFixture.createOrganization();
        organizationId = organization.getId();
        OrganizationContext.set(organizationId);
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
    void commitFailsWhenLinesInsertedDirectlyDoNotBalance() throws SQLException {
        UUID entryId = UUID.randomUUID();

        assertThatThrownBy(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                insertJournalEntry(connection, entryId);
                insertLine(connection, entryId, bank.getId(), "100.00", "0");
                insertLine(connection, entryId, revenue.getId(), "0", "75.00");
                connection.commit();
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("not balanced");
    }

    private void insertJournalEntry(Connection connection, UUID entryId) throws SQLException {
        String sql = """
                insert into journal_entries
                    (id, version, created_at, updated_at, organization_id, entry_number, entry_date,
                     transaction_type, status, currency_code)
                values (?, 0, now(), now(), ?, ?, ?, 'JOURNAL_ENTRY', 'POSTED', 'USD')
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, entryId);
            ps.setObject(2, organizationId);
            ps.setString(3, "JE-TRG-" + entryId.toString().substring(0, 8));
            ps.setObject(4, LocalDate.now());
            ps.executeUpdate();
        }
    }

    private void insertLine(Connection connection, UUID entryId, UUID accountId, String debit, String credit)
            throws SQLException {
        String sql = """
                insert into journal_entry_lines
                    (id, version, created_at, updated_at, journal_entry_id, line_number, account_id,
                     debit_amount, credit_amount)
                values (?, 0, now(), now(), ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, entryId);
            ps.setInt(3, 1);
            ps.setObject(4, accountId);
            ps.setBigDecimal(5, new java.math.BigDecimal(debit));
            ps.setBigDecimal(6, new java.math.BigDecimal(credit));
            ps.executeUpdate();
        }
    }
}
