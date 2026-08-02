import axios, { AxiosError } from 'axios'
import { useOrgStore } from '@/store/useOrgStore'
import type { ApiErrorResponse } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string
  readonly fieldErrors: ApiErrorResponse['fieldErrors']

  constructor(response: ApiErrorResponse) {
    super(response.message)
    this.name = 'ApiError'
    this.status = response.status
    this.errorCode = response.errorCode
    this.fieldErrors = response.fieldErrors
  }
}

export const apiClient = axios.create({
  baseURL: '/api/v1',
})

apiClient.interceptors.request.use((config) => {
  const organizationId = useOrgStore.getState().currentOrganizationId
  if (organizationId) {
    config.headers['X-Organization-Id'] = organizationId
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response?.data?.message) {
      return Promise.reject(new ApiError(error.response.data))
    }
    return Promise.reject(error)
  },
)
