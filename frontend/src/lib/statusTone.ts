const STATUS_TONES: Record<string, 'slate' | 'blue' | 'green' | 'amber' | 'red' | 'purple'> = {
  DRAFT: 'slate',
  SENT: 'blue',
  RECEIVED: 'blue',
  POSTED: 'blue',
  PARTIALLY_PAID: 'amber',
  OVERDUE: 'red',
  PAID: 'green',
  RECONCILED: 'green',
  ACTIVE: 'green',
  LOCKED: 'purple',
  CANCELLED: 'slate',
  VOID: 'slate',
  INACTIVE: 'slate',
  ARCHIVED: 'slate',
  UNMATCHED: 'amber',
  MATCHED: 'green',
  IGNORED: 'slate',
  IN_PROGRESS: 'amber',
  COMPLETED: 'green',
}

export function statusTone(status: string) {
  return STATUS_TONES[status] ?? 'slate'
}
