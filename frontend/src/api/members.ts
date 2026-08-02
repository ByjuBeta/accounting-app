import { apiClient } from './client'
import type { Role } from './auth'

export interface Member {
  id: string
  userId: string
  email: string
  firstName: string
  lastName: string
  role: Role
  active: boolean
}

export interface AddMemberRequest {
  email: string
  role: Role
}

export const membersApi = {
  list: async (): Promise<Member[]> => {
    const { data } = await apiClient.get<Member[]>('/members')
    return data
  },
  add: async (request: AddMemberRequest): Promise<Member> => {
    const { data } = await apiClient.post<Member>('/members', request)
    return data
  },
  updateRole: async (id: string, role: Role): Promise<Member> => {
    const { data } = await apiClient.put<Member>(`/members/${id}/role`, { role })
    return data
  },
  remove: async (id: string): Promise<void> => {
    await apiClient.delete(`/members/${id}`)
  },
}
