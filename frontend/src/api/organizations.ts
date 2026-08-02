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

export const organizationsApi = {
  list: async (): Promise<Organization[]> => {
    const { data } = await apiClient.get<Organization[]>('/organizations')
    return data
  },
  create: async (request: CreateOrganizationRequest): Promise<Organization> => {
    const { data } = await apiClient.post<Organization>('/organizations', request)
    return data
  },
}
