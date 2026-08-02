import { apiClient } from './client'
import type { Page } from './types'

export type TransactionType =
  'JOURNAL_ENTRY' | 'INVOICE' | 'BILL' | 'CHECK' | 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER'
export type TransactionStatus = 'DRAFT' | 'POSTED' | 'RECONCILED' | 'LOCKED' | 'VOID'

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  JOURNAL_ENTRY: 'Journal Entry',
  INVOICE: 'Invoice',
  BILL: 'Bill',
  CHECK: 'Check',
  DEPOSIT: 'Deposit',
  WITHDRAWAL: 'Withdrawal',
  TRANSFER: 'Transfer',
}

export interface JournalEntryLine {
  id: string
  lineNumber: number
  accountId: string
  accountCode: string
  accountName: string
  debitAmount: string
  creditAmount: string
  memo: string | null
  tags: string[]
  reconciled: boolean
}

export interface JournalEntry {
  id: string
  version: number
  entryNumber: string
  entryDate: string
  transactionType: TransactionType
  status: TransactionStatus
  memo: string | null
  referenceNumber: string | null
  currencyCode: string
  postedAt: string | null
  reversalOfId: string | null
  voidedAt: string | null
  voidReason: string | null
  totalDebit: string
  totalCredit: string
  lines: JournalEntryLine[]
}

export interface JournalEntryLineRequest {
  accountId: string
  debitAmount: number
  creditAmount: number
  memo?: string
  tags?: string[]
}

export interface CreateJournalEntryRequest {
  entryDate: string
  transactionType: TransactionType
  memo?: string
  referenceNumber?: string
  currencyCode?: string
  lines: JournalEntryLineRequest[]
}

export const journalEntriesApi = {
  list: async (page = 0, size = 25): Promise<Page<JournalEntry>> => {
    const { data } = await apiClient.get<Page<JournalEntry>>('/journal-entries', {
      params: { page, size, sort: 'entryDate,desc' },
    })
    return data
  },
  get: async (id: string): Promise<JournalEntry> => {
    const { data } = await apiClient.get<JournalEntry>(`/journal-entries/${id}`)
    return data
  },
  create: async (request: CreateJournalEntryRequest): Promise<JournalEntry> => {
    const { data } = await apiClient.post<JournalEntry>('/journal-entries', request)
    return data
  },
  update: async (id: string, request: CreateJournalEntryRequest): Promise<JournalEntry> => {
    const { data } = await apiClient.put<JournalEntry>(`/journal-entries/${id}`, request)
    return data
  },
  remove: async (id: string): Promise<void> => {
    await apiClient.delete(`/journal-entries/${id}`)
  },
  post: async (id: string): Promise<JournalEntry> => {
    const { data } = await apiClient.post<JournalEntry>(`/journal-entries/${id}/post`)
    return data
  },
  void: async (id: string, reason: string): Promise<JournalEntry> => {
    const { data } = await apiClient.post<JournalEntry>(`/journal-entries/${id}/void`, { reason })
    return data
  },
  reverse: async (id: string, reversalDate?: string): Promise<JournalEntry> => {
    const { data } = await apiClient.post<JournalEntry>(`/journal-entries/${id}/reverse`, { reversalDate })
    return data
  },
}
