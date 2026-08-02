import { apiClient } from './client'

export type Role = 'ADMIN' | 'MANAGER' | 'ACCOUNTANT' | 'VIEWER' | 'LIMITED_USER'

export interface UserDto {
  id: string
  email: string
  firstName: string
  lastName: string
}

export interface MembershipDto {
  organizationId: string
  organizationName: string
  role: Role
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  user: UserDto
  memberships: MembershipDto[]
}

export interface MeResponse {
  user: UserDto
  memberships: MembershipDto[]
}

export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
}

export interface LoginRequest {
  email: string
  password: string
}

export const authApi = {
  register: async (request: RegisterRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>('/auth/register', request)
    return data
  },
  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>('/auth/login', request)
    return data
  },
  logout: async (refreshToken: string): Promise<void> => {
    await apiClient.post('/auth/logout', { refreshToken })
  },
  me: async (): Promise<MeResponse> => {
    const { data } = await apiClient.get<MeResponse>('/auth/me')
    return data
  },
}
