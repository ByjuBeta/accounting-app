package com.accountingapp.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrialBalance(LocalDate asOfDate, List<AccountBalance> rows, BigDecimal totalDebit, BigDecimal totalCredit) {

    public boolean isBalanced() {
        return totalDebit.compareTo(totalCredit) == 0;
    }
}
