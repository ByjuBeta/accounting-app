package com.accountingapp.account;

/**
 * The specific subtype a {@code Account} is opened as. Each carries the GAAP
 * {@link AccountCategory} it rolls up into and its {@link NormalBalance} —
 * most types follow their category's normal balance, but contra accounts
 * (e.g. accumulated depreciation) deliberately don't.
 */
public enum AccountType {
    BANK(AccountCategory.ASSET, NormalBalance.DEBIT),
    CASH(AccountCategory.ASSET, NormalBalance.DEBIT),
    ACCOUNTS_RECEIVABLE(AccountCategory.ASSET, NormalBalance.DEBIT),
    OTHER_CURRENT_ASSET(AccountCategory.ASSET, NormalBalance.DEBIT),
    FIXED_ASSET(AccountCategory.ASSET, NormalBalance.DEBIT),
    ACCUMULATED_DEPRECIATION(AccountCategory.ASSET, NormalBalance.CREDIT),

    ACCOUNTS_PAYABLE(AccountCategory.LIABILITY, NormalBalance.CREDIT),
    CREDIT_CARD(AccountCategory.LIABILITY, NormalBalance.CREDIT),
    OTHER_CURRENT_LIABILITY(AccountCategory.LIABILITY, NormalBalance.CREDIT),
    LONG_TERM_LIABILITY(AccountCategory.LIABILITY, NormalBalance.CREDIT),

    EQUITY(AccountCategory.EQUITY, NormalBalance.CREDIT),

    INCOME(AccountCategory.REVENUE, NormalBalance.CREDIT),

    COST_OF_GOODS_SOLD(AccountCategory.EXPENSE, NormalBalance.DEBIT),
    EXPENSE(AccountCategory.EXPENSE, NormalBalance.DEBIT);

    private final AccountCategory category;
    private final NormalBalance normalBalance;

    AccountType(AccountCategory category, NormalBalance normalBalance) {
        this.category = category;
        this.normalBalance = normalBalance;
    }

    public AccountCategory getCategory() {
        return category;
    }

    public NormalBalance getNormalBalance() {
        return normalBalance;
    }
}
