import { apiClient } from './client'

export type AccountCategory = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE'
export type NormalBalance = 'DEBIT' | 'CREDIT'
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'

export type AccountType =
  | 'BANK'
  | 'CASH'
  | 'ACCOUNTS_RECEIVABLE'
  | 'OTHER_CURRENT_ASSET'
  | 'FIXED_ASSET'
  | 'ACCUMULATED_DEPRECIATION'
  | 'ACCOUNTS_PAYABLE'
  | 'CREDIT_CARD'
  | 'OTHER_CURRENT_LIABILITY'
  | 'LONG_TERM_LIABILITY'
  | 'EQUITY'
  | 'INCOME'
  | 'COST_OF_GOODS_SOLD'
  | 'EXPENSE'

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  BANK: 'Bank',
  CASH: 'Cash',
  ACCOUNTS_RECEIVABLE: 'Accounts Receivable',
  OTHER_CURRENT_ASSET: 'Other Current Asset',
  FIXED_ASSET: 'Fixed Asset',
  ACCUMULATED_DEPRECIATION: 'Accumulated Depreciation',
  ACCOUNTS_PAYABLE: 'Accounts Payable',
  CREDIT_CARD: 'Credit Card',
  OTHER_CURRENT_LIABILITY: 'Other Current Liability',
  LONG_TERM_LIABILITY: 'Long-Term Liability',
  EQUITY: 'Equity',
  INCOME: 'Income',
  COST_OF_GOODS_SOLD: 'Cost of Goods Sold',
  EXPENSE: 'Expense',
}

export interface Account {
  id: string
  version: number
  code: string
  name: string
  description: string | null
  accountType: AccountType
  category: AccountCategory
  normalBalance: NormalBalance
  parentId: string | null
  currencyCode: string
  status: AccountStatus
  tags: string[]
}

export interface CreateAccountRequest {
  code: string
  name: string
  description?: string
  accountType: AccountType
  parentId?: string | null
  currencyCode?: string
  tags?: string[]
  openingBalance?: number
  openingBalanceDate?: string
}

export interface UpdateAccountRequest {
  code: string
  name: string
  description?: string
  parentId?: string | null
  status?: AccountStatus
  tags?: string[]
}

export const accountsApi = {
  list: async (status?: AccountStatus): Promise<Account[]> => {
    const { data } = await apiClient.get<Account[]>('/chart-of-accounts', { params: { status } })
    return data
  },
  get: async (id: string): Promise<Account> => {
    const { data } = await apiClient.get<Account>(`/chart-of-accounts/${id}`)
    return data
  },
  create: async (request: CreateAccountRequest): Promise<Account> => {
    const { data } = await apiClient.post<Account>('/chart-of-accounts', request)
    return data
  },
  update: async (id: string, request: UpdateAccountRequest): Promise<Account> => {
    const { data } = await apiClient.put<Account>(`/chart-of-accounts/${id}`, request)
    return data
  },
  remove: async (id: string): Promise<void> => {
    await apiClient.delete(`/chart-of-accounts/${id}`)
  },
}
