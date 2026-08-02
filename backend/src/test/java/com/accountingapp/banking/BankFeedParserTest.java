package com.accountingapp.banking;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.banking.BankFeedParser.ParsedTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BankFeedParserTest {

    private final BankFeedParser parser = new BankFeedParser();

    @Test
    void parsesCsvWithHeaderAndQuotedDescription() {
        String csv = """
                Date,Description,Amount,CheckNumber
                2026-01-15,"Coffee, Shop",-4.50,
                2026-01-16,Payroll Deposit,2500.00,1042
                """;

        List<ParsedTransaction> transactions = parser.parseCsv(csv);

        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("-4.50"));
        assertThat(transactions.get(0).description()).isEqualTo("Coffee, Shop");
        assertThat(transactions.get(1).checkNumber()).isEqualTo("1042");
    }

    @Test
    void csvRowsWithSameDateAmountDescriptionHashToTheSameExternalId() {
        String csv = """
                Date,Description,Amount
                2026-01-15,Coffee Shop,-4.50
                2026-01-15,Coffee Shop,-4.50
                """;

        List<ParsedTransaction> transactions = parser.parseCsv(csv);

        assertThat(transactions.get(0).externalId()).isEqualTo(transactions.get(1).externalId());
    }

    @Test
    void parsesOfxStatementTransactions() {
        String ofx = """
                <OFX>
                <BANKTRANLIST>
                <STMTTRN>
                <TRNTYPE>DEBIT
                <DTPOSTED>20260115120000
                <TRNAMT>-42.50
                <FITID>202601150001
                <NAME>Coffee Shop
                </STMTTRN>
                <STMTTRN>
                <TRNTYPE>CREDIT
                <DTPOSTED>20260116
                <TRNAMT>2500.00
                <FITID>202601160001
                <NAME>Payroll Deposit
                <CHECKNUM>1042
                </STMTTRN>
                </BANKTRANLIST>
                </OFX>
                """;

        List<ParsedTransaction> transactions = parser.parseOfx(ofx);

        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("-42.50"));
        assertThat(transactions.get(0).externalId()).isEqualTo("202601150001");
        assertThat(transactions.get(1).checkNumber()).isEqualTo("1042");
    }
}
