package com.accountingapp.ledger;

import com.accountingapp.account.AccountType;
import com.accountingapp.account.NormalBalance;
import java.math.BigDecimal;
import java.util.UUID;

/** An account's activity as of a point in time, with {@code balance} signed positive in its normal-balance direction. */
public record AccountBalance(
        UUID accountId,
        String accountCode,
        String accountName,
        AccountType accountType,
        NormalBalance normalBalance,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal balance) {

    public static AccountBalance of(
            UUID accountId, String code, String name, AccountType type, BigDecimal debit, BigDecimal credit) {
        NormalBalance normalBalance = type.getNormalBalance();
        BigDecimal balance = normalBalance == NormalBalance.DEBIT
                ? debit.subtract(credit)
                : credit.subtract(debit);
        return new AccountBalance(accountId, code, name, type, normalBalance, debit, credit, balance);
    }
}
