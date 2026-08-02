import { apiClient } from './client'
import type { AgingBucket } from './arAging'

export interface VendorAging {
  vendorId: string
  vendorName: string
  bucket: AgingBucket
}

export interface ApAgingReport {
  asOfDate: string
  rows: VendorAging[]
  totals: AgingBucket
}

export const apAgingApi = {
  get: async (asOfDate?: string): Promise<ApAgingReport> => {
    const { data } = await apiClient.get<ApAgingReport>('/accounts-payable/aging', {
      params: { asOfDate },
    })
    return data
  },
}
