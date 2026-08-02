package com.accountingapp.ar;

/**
 * Draft -> Sent -> [Partially Paid] -> Paid, or Draft/Sent -> Cancelled.
 * Overdue is not a stored state — it's derived from due date vs. today
 * whenever the invoice is unpaid (see {@link Invoice#getEffectiveStatus}).
 */
public enum InvoiceStatus {
    DRAFT,
    SENT,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED
}
