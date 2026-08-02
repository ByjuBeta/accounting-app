import { apiClient } from './client'
import type { Page } from './types'

export interface PaymentApplication {
  invoiceId: string
  invoiceNumber: string
  amountApplied: string
}

export interface Payment {
  id: string
  version: number
  paymentNumber: string
  customerId: string
  customerName: string
  paymentDate: string
  amount: string
  depositToAccountId: string
  memo: string | null
  referenceNumber: string | null
  totalApplied: string
  unappliedAmount: string
  applications: PaymentApplication[]
}

export interface PaymentApplicationRequest {
  invoiceId: string
  amount: number
}

export interface CreatePaymentRequest {
  customerId: string
  paymentDate: string
  amount: number
  depositToAccountId: string
  memo?: string
  referenceNumber?: string
  applications: PaymentApplicationRequest[]
}

export const paymentsApi = {
  list: async (page = 0, size = 25): Promise<Page<Payment>> => {
    const { data } = await apiClient.get<Page<Payment>>('/payments', {
      params: { page, size, sort: 'paymentDate,desc' },
    })
    return data
  },
  get: async (id: string): Promise<Payment> => {
    const { data } = await apiClient.get<Payment>(`/payments/${id}`)
    return data
  },
  create: async (request: CreatePaymentRequest): Promise<Payment> => {
    const { data } = await apiClient.post<Payment>('/payments', request)
    return data
  },
}
