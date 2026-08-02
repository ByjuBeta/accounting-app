import { apiClient } from './client'
import type { Page } from './types'

export type BillStatus = 'DRAFT' | 'RECEIVED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED'

export interface BillLine {
  id: string
  lineNumber: number
  description: string
  quantity: string
  unitPrice: string
  taxRate: string
  expenseAccountId: string
  expenseAccountCode: string
  lineSubtotal: string
  lineTax: string
  lineTotal: string
}

export interface Bill {
  id: string
  version: number
  billNumber: string
  vendorReferenceNumber: string | null
  vendorId: string
  vendorName: string
  billDate: string
  dueDate: string
  status: BillStatus
  effectiveStatus: BillStatus
  memo: string | null
  currencyCode: string
  subtotal: string
  taxTotal: string
  total: string
  amountPaid: string
  balanceDue: string
  eligibleForEarlyPaymentDiscountToday: boolean
  earlyPaymentDiscountAmount: string
  lines: BillLine[]
}

export interface BillLineRequest {
  description: string
  quantity: number
  unitPrice: number
  taxRate?: number
  expenseAccountId: string
}

export interface CreateBillRequest {
  vendorId: string
  vendorReferenceNumber?: string
  billDate: string
  dueDate?: string
  memo?: string
  currencyCode?: string
  lines: BillLineRequest[]
}

export const billsApi = {
  list: async (page = 0, size = 25): Promise<Page<Bill>> => {
    const { data } = await apiClient.get<Page<Bill>>('/bills', {
      params: { page, size, sort: 'dueDate,asc' },
    })
    return data
  },
  get: async (id: string): Promise<Bill> => {
    const { data } = await apiClient.get<Bill>(`/bills/${id}`)
    return data
  },
  listOpen: async (): Promise<Bill[]> => {
    const { data } = await apiClient.get<Bill[]>('/bills/open')
    return data
  },
  create: async (request: CreateBillRequest): Promise<Bill> => {
    const { data } = await apiClient.post<Bill>('/bills', request)
    return data
  },
  receive: async (id: string): Promise<Bill> => {
    const { data } = await apiClient.post<Bill>(`/bills/${id}/receive`)
    return data
  },
  cancel: async (id: string): Promise<Bill> => {
    const { data } = await apiClient.post<Bill>(`/bills/${id}/cancel`)
    return data
  },
}
