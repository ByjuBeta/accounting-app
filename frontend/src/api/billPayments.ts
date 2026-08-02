import { apiClient } from './client'
import type { Page } from './types'

export interface BillPaymentApplication {
  billId: string
  billNumber: string
  amountApplied: string
  discountTaken: string
}

export interface BillPayment {
  id: string
  version: number
  paymentNumber: string
  vendorId: string
  vendorName: string
  paymentDate: string
  amount: string
  paidFromAccountId: string
  memo: string | null
  referenceNumber: string | null
  totalApplied: string
  totalDiscountTaken: string
  unappliedAmount: string
  applications: BillPaymentApplication[]
}

export interface BillPaymentApplicationRequest {
  billId: string
  amount: number
  discountTaken?: number
}

export interface CreateBillPaymentRequest {
  vendorId: string
  paymentDate: string
  amount: number
  paidFromAccountId: string
  memo?: string
  referenceNumber?: string
  applications: BillPaymentApplicationRequest[]
}

export const billPaymentsApi = {
  list: async (page = 0, size = 25): Promise<Page<BillPayment>> => {
    const { data } = await apiClient.get<Page<BillPayment>>('/bill-payments', {
      params: { page, size, sort: 'paymentDate,desc' },
    })
    return data
  },
  get: async (id: string): Promise<BillPayment> => {
    const { data } = await apiClient.get<BillPayment>(`/bill-payments/${id}`)
    return data
  },
  create: async (request: CreateBillPaymentRequest): Promise<BillPayment> => {
    const { data } = await apiClient.post<BillPayment>('/bill-payments', request)
    return data
  },
}
