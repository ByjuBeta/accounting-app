import { apiClient } from './client'

export interface AgingBucket {
  current: string
  days1to30: string
  days31to60: string
  days61to90: string
  over90: string
}

export interface CustomerAging {
  customerId: string
  customerName: string
  bucket: AgingBucket
}

export interface ArAgingReport {
  asOfDate: string
  rows: CustomerAging[]
  totals: AgingBucket
}

export const arAgingApi = {
  get: async (asOfDate?: string): Promise<ArAgingReport> => {
    const { data } = await apiClient.get<ArAgingReport>('/accounts-receivable/aging', {
      params: { asOfDate },
    })
    return data
  },
}
