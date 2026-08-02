import { apiClient } from './client'
import type { Address } from './address'

export type VendorStatus = 'ACTIVE' | 'INACTIVE'

export interface Vendor {
  id: string
  version: number
  name: string
  companyName: string | null
  email: string | null
  phone: string | null
  address: Address | null
  paymentTermsDays: number
  earlyPaymentDiscountPercent: string
  earlyPaymentDiscountDays: number
  taxId: string | null
  notes: string | null
  status: VendorStatus
}

export interface CreateVendorRequest {
  name: string
  companyName?: string
  email?: string
  phone?: string
  address?: Address
  paymentTermsDays?: number
  earlyPaymentDiscountPercent?: number
  earlyPaymentDiscountDays?: number
  taxId?: string
  notes?: string
}

export interface UpdateVendorRequest extends CreateVendorRequest {
  status?: VendorStatus
}

export const vendorsApi = {
  list: async (): Promise<Vendor[]> => {
    const { data } = await apiClient.get<Vendor[]>('/vendors')
    return data
  },
  get: async (id: string): Promise<Vendor> => {
    const { data } = await apiClient.get<Vendor>(`/vendors/${id}`)
    return data
  },
  create: async (request: CreateVendorRequest): Promise<Vendor> => {
    const { data } = await apiClient.post<Vendor>('/vendors', request)
    return data
  },
  update: async (id: string, request: UpdateVendorRequest): Promise<Vendor> => {
    const { data } = await apiClient.put<Vendor>(`/vendors/${id}`, request)
    return data
  },
}
