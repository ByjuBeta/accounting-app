import { apiClient } from './client'

export interface Organization {
  id: string
  name: string
  legalName: string | null
  baseCurrencyCode: string
  timeZone: string
  active: boolean
}

export interface CreateOrganizationRequest {
  name: string
  legalName?: string
  baseCurrencyCode: string
  timeZone: string
}

export interface UpdateOrganizationRequest {
  name: string
  legalName?: string
  timeZone: string
}

export const organizationsApi = {
  list: async (): Promise<Organization[]> => {
    const { data } = await apiClient.get<Organization[]>('/organizations')
    return data
  },
  get: async (id: string): Promise<Organization> => {
    const { data } = await apiClient.get<Organization>(`/organizations/${id}`)
    return data
  },
  create: async (request: CreateOrganizationRequest): Promise<Organization> => {
    const { data } = await apiClient.post<Organization>('/organizations', request)
    return data
  },
  update: async (id: string, request: UpdateOrganizationRequest): Promise<Organization> => {
    const { data } = await apiClient.put<Organization>(`/organizations/${id}`, request)
    return data
  },
}
