package com.accountingapp.journal;

/** Draft -> Posted -> Reconciled -> Locked, with Void reachable from Draft or Posted. */
public enum TransactionStatus {
    DRAFT,
    POSTED,
    RECONCILED,
    LOCKED,
    VOID
}
