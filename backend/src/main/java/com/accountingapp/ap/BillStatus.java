package com.accountingapp.ap;

/**
 * Draft -> Received -> [Partially Paid] -> Paid, or Draft/Received -> Cancelled.
 * Overdue is derived from due date vs. today, not stored — see {@link Bill#getEffectiveStatus}.
 */
public enum BillStatus {
    DRAFT,
    RECEIVED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED
}
