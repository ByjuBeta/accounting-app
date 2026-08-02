import { apiClient } from './client'
import type { Address } from './address'

export type CustomerStatus = 'ACTIVE' | 'INACTIVE'

export interface Customer {
  id: string
  version: number
  name: string
  companyName: string | null
  email: string | null
  phone: string | null
  billingAddress: Address | null
  paymentTermsDays: number
  taxExempt: boolean
  notes: string | null
  status: CustomerStatus
}

export interface CreateCustomerRequest {
  name: string
  companyName?: string
  email?: string
  phone?: string
  billingAddress?: Address
  paymentTermsDays?: number
  taxExempt: boolean
  notes?: string
}

export interface UpdateCustomerRequest extends CreateCustomerRequest {
  status?: CustomerStatus
}

export const customersApi = {
  list: async (): Promise<Customer[]> => {
    const { data } = await apiClient.get<Customer[]>('/customers')
    return data
  },
  get: async (id: string): Promise<Customer> => {
    const { data } = await apiClient.get<Customer>(`/customers/${id}`)
    return data
  },
  create: async (request: CreateCustomerRequest): Promise<Customer> => {
    const { data } = await apiClient.post<Customer>('/customers', request)
    return data
  },
  update: async (id: string, request: UpdateCustomerRequest): Promise<Customer> => {
    const { data } = await apiClient.put<Customer>(`/customers/${id}`, request)
    return data
  },
}
