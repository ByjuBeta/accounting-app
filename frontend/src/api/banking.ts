import { apiClient } from './client'

export type BankTransactionStatus = 'UNMATCHED' | 'MATCHED' | 'IGNORED'
export type ReconciliationStatus = 'IN_PROGRESS' | 'COMPLETED'
export type BankFeedFormat = 'CSV' | 'OFX'

export interface BankAccountDetail {
  id: string
  accountId: string
  bankName: string | null
  routingNumberLast4: string | null
  accountNumberLast4: string | null
  notes: string | null
}

export interface UpsertBankAccountDetailRequest {
  bankName?: string
  routingNumberLast4?: string
  accountNumberLast4?: string
  notes?: string
}

export interface BankTransaction {
  id: string
  accountId: string
  transactionDate: string
  amount: string
  description: string
  checkNumber: string | null
  status: BankTransactionStatus
  matchedJournalEntryLineId: string | null
}

export interface JournalEntryLineCandidate {
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

export interface BankImportResult {
  imported: number
  skippedDuplicates: number
}

export interface OutstandingLine {
  journalEntryLineId: string
  journalEntryId: string
  entryNumber: string
  entryDate: string
  description: string | null
  amount: string
}

export interface Reconciliation {
  id: string
  accountId: string
  statementDate: string
  beginningBalance: string
  statementEndingBalance: string
  clearedBalance: string | null
  status: ReconciliationStatus
  completedAt: string | null
}

export interface ReconciliationWorksheet {
  reconciliationId: string
  accountId: string
  statementDate: string
  status: ReconciliationStatus
  beginningBalance: string
  statementEndingBalance: string
  clearedTotal: string
  projectedEndingBalance: string
  difference: string
  clearedTransactionCount: number
  outstandingLines: OutstandingLine[]
}

export interface StartReconciliationRequest {
  statementDate: string
  statementEndingBalance: number
}

export const bankAccountDetailsApi = {
  get: async (accountId: string): Promise<BankAccountDetail> => {
    const { data } = await apiClient.get<BankAccountDetail>(`/bank-accounts/${accountId}/details`)
    return data
  },
  upsert: async (accountId: string, request: UpsertBankAccountDetailRequest): Promise<BankAccountDetail> => {
    const { data } = await apiClient.put<BankAccountDetail>(`/bank-accounts/${accountId}/details`, request)
    return data
  },
}

export const bankTransactionsApi = {
  list: async (accountId: string, status?: BankTransactionStatus): Promise<BankTransaction[]> => {
    const { data } = await apiClient.get<BankTransaction[]>('/bank-transactions', {
      params: { accountId, status },
    })
    return data
  },
  importFeed: async (accountId: string, format: BankFeedFormat, file: File): Promise<BankImportResult> => {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await apiClient.post<BankImportResult>('/bank-transactions/import', formData, {
      params: { accountId, format },
    })
    return data
  },
  suggestedMatches: async (id: string): Promise<JournalEntryLineCandidate[]> => {
    const { data } = await apiClient.get<JournalEntryLineCandidate[]>(
      `/bank-transactions/${id}/suggested-matches`,
    )
    return data
  },
  autoMatch: async (accountId: string): Promise<{ matched: number }> => {
    const { data } = await apiClient.post<{ matched: number }>('/bank-transactions/auto-match', null, {
      params: { accountId },
    })
    return data
  },
  match: async (id: string, journalEntryLineId: string): Promise<BankTransaction> => {
    const { data } = await apiClient.post<BankTransaction>(`/bank-transactions/${id}/match`, {
      journalEntryLineId,
    })
    return data
  },
  unmatch: async (id: string): Promise<BankTransaction> => {
    const { data } = await apiClient.post<BankTransaction>(`/bank-transactions/${id}/unmatch`)
    return data
  },
  ignore: async (id: string): Promise<BankTransaction> => {
    const { data } = await apiClient.post<BankTransaction>(`/bank-transactions/${id}/ignore`)
    return data
  },
}

export const reconciliationsApi = {
  start: async (accountId: string, request: StartReconciliationRequest): Promise<Reconciliation> => {
    const { data } = await apiClient.post<Reconciliation>('/reconciliations', request, {
      params: { accountId },
    })
    return data
  },
  get: async (id: string): Promise<Reconciliation> => {
    const { data } = await apiClient.get<Reconciliation>(`/reconciliations/${id}`)
    return data
  },
  worksheet: async (id: string): Promise<ReconciliationWorksheet> => {
    const { data } = await apiClient.get<ReconciliationWorksheet>(`/reconciliations/${id}/worksheet`)
    return data
  },
  complete: async (id: string): Promise<Reconciliation> => {
    const { data } = await apiClient.post<Reconciliation>(`/reconciliations/${id}/complete`)
    return data
  },
  history: async (accountId: string): Promise<Reconciliation[]> => {
    const { data } = await apiClient.get<Reconciliation[]>('/reconciliations', { params: { accountId } })
    return data
  },
}
