import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useOrgStore } from '@/store/useOrgStore'
import { useAuthStore } from '@/store/useAuthStore'
import type { ApiErrorResponse } from './types'
import type { MembershipDto, UserDto } from './auth'

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
  const accessToken = useAuthStore.getState().accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  const organizationId = useOrgStore.getState().currentOrganizationId
  if (organizationId) {
    config.headers['X-Organization-Id'] = organizationId
  }
  return config
})

interface RefreshResponseBody {
  accessToken: string
  refreshToken: string
  user: UserDto
  memberships: MembershipDto[]
}

let refreshPromise: Promise<string | null> | null = null

/** Uses a bare axios call (not `apiClient`) so this never re-enters the response interceptor below. */
async function refreshAccessToken(): Promise<string | null> {
  const { refreshToken, setAuth, clear } = useAuthStore.getState()
  if (!refreshToken) return null
  try {
    const { data } = await axios.post<RefreshResponseBody>('/api/v1/auth/refresh', { refreshToken })
    setAuth(data)
    return data.accessToken
  } catch {
    clear()
    return null
  }
}

const AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh']

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const isAuthEndpoint = AUTH_PATHS.some((path) => original?.url?.includes(path))

    if (error.response?.status === 401 && original && !original._retried && !isAuthEndpoint) {
      original._retried = true
      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null
      })
      const newAccessToken = await refreshPromise
      if (newAccessToken) {
        original.headers.set('Authorization', `Bearer ${newAccessToken}`)
        return apiClient.request(original)
      }
    }

    if (error.response?.data?.message) {
      return Promise.reject(new ApiError(error.response.data))
    }
    return Promise.reject(error)
  },
)
