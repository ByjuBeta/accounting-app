import { apiClient } from './client'

export interface AccountBalanceLine {
  accountId: string
  code: string
  name: string
  balance: string
}

export interface BalanceSheetSection {
  label: string
  lines: AccountBalanceLine[]
  total: string
}

export interface BalanceSheet {
  asOfDate: string
  assets: BalanceSheetSection
  liabilities: BalanceSheetSection
  equity: BalanceSheetSection
  netIncomeToDate: string
  totalLiabilitiesAndEquity: string
  balanced: boolean
}

export interface IncomeStatement {
  fromDate: string
  toDate: string
  revenue: AccountBalanceLine[]
  expenses: AccountBalanceLine[]
  totalRevenue: string
  totalExpense: string
  netIncome: string
}

export type CashFlowMethod = 'DIRECT' | 'INDIRECT'

export interface CashFlowLine {
  label: string
  amount: string
}

export interface CashFlowStatement {
  fromDate: string
  toDate: string
  method: CashFlowMethod
  operatingLines: CashFlowLine[]
  operatingTotal: string
  investingLines: CashFlowLine[]
  investingTotal: string
  financingLines: CashFlowLine[]
  financingTotal: string
  netChangeInCash: string
  beginningCash: string
  endingCash: string
}

export interface GeneralLedgerLine {
  journalEntryLineId: string
  journalEntryId: string
  entryNumber: string
  entryDate: string
  memo: string | null
  debit: string
  credit: string
  runningBalance: string
}

export interface GeneralLedgerDetail {
  accountId: string
  accountCode: string
  accountName: string
  fromDate: string
  toDate: string
  beginningBalance: string
  lines: GeneralLedgerLine[]
  endingBalance: string
}

export const reportsApi = {
  balanceSheet: async (asOfDate?: string): Promise<BalanceSheet> => {
    const { data } = await apiClient.get<BalanceSheet>('/reports/balance-sheet', { params: { asOfDate } })
    return data
  },
  incomeStatement: async (fromDate: string, toDate: string): Promise<IncomeStatement> => {
    const { data } = await apiClient.get<IncomeStatement>('/reports/income-statement', {
      params: { fromDate, toDate },
    })
    return data
  },
  cashFlow: async (fromDate: string, toDate: string, method: CashFlowMethod): Promise<CashFlowStatement> => {
    const { data } = await apiClient.get<CashFlowStatement>('/reports/cash-flow', {
      params: { fromDate, toDate, method },
    })
    return data
  },
  generalLedger: async (
    accountId: string,
    fromDate: string,
    toDate: string,
  ): Promise<GeneralLedgerDetail> => {
    const { data } = await apiClient.get<GeneralLedgerDetail>(`/reports/general-ledger/${accountId}`, {
      params: { fromDate, toDate },
    })
    return data
  },
}
