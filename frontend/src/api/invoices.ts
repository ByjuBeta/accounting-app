import { apiClient } from './client'
import type { Page } from './types'

export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED'

export interface InvoiceLine {
  id: string
  lineNumber: number
  description: string
  quantity: string
  unitPrice: string
  taxRate: string
  incomeAccountId: string
  incomeAccountCode: string
  lineSubtotal: string
  lineTax: string
  lineTotal: string
}

export interface Invoice {
  id: string
  version: number
  invoiceNumber: string
  customerId: string
  customerName: string
  invoiceDate: string
  dueDate: string
  status: InvoiceStatus
  effectiveStatus: InvoiceStatus
  memo: string | null
  currencyCode: string
  subtotal: string
  taxTotal: string
  total: string
  amountPaid: string
  balanceDue: string
  lines: InvoiceLine[]
}

export interface InvoiceLineRequest {
  description: string
  quantity: number
  unitPrice: number
  taxRate?: number
  incomeAccountId: string
}

export interface CreateInvoiceRequest {
  customerId: string
  invoiceDate: string
  dueDate?: string
  memo?: string
  currencyCode?: string
  lines: InvoiceLineRequest[]
}

export const invoicesApi = {
  list: async (page = 0, size = 25): Promise<Page<Invoice>> => {
    const { data } = await apiClient.get<Page<Invoice>>('/invoices', {
      params: { page, size, sort: 'invoiceDate,desc' },
    })
    return data
  },
  get: async (id: string): Promise<Invoice> => {
    const { data } = await apiClient.get<Invoice>(`/invoices/${id}`)
    return data
  },
  listOpen: async (): Promise<Invoice[]> => {
    const { data } = await apiClient.get<Invoice[]>('/invoices/open')
    return data
  },
  create: async (request: CreateInvoiceRequest): Promise<Invoice> => {
    const { data } = await apiClient.post<Invoice>('/invoices', request)
    return data
  },
  send: async (id: string): Promise<Invoice> => {
    const { data } = await apiClient.post<Invoice>(`/invoices/${id}/send`)
    return data
  },
  cancel: async (id: string): Promise<Invoice> => {
    const { data } = await apiClient.post<Invoice>(`/invoices/${id}/cancel`)
    return data
  },
}
