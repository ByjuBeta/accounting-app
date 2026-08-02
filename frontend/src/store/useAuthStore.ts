import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { useOrgStore } from './useOrgStore'
import type { MembershipDto, UserDto } from '@/api/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserDto | null
  memberships: MembershipDto[]
  setAuth: (params: {
    accessToken: string
    refreshToken: string
    user: UserDto
    memberships: MembershipDto[]
  }) => void
  setMemberships: (memberships: MembershipDto[]) => void
  clear: () => void
}

/** Drops the current org selection if it's no longer among the given memberships, picking the first instead. */
function reconcileCurrentOrganization(memberships: MembershipDto[]) {
  const orgStore = useOrgStore.getState()
  const stillValid = memberships.some((m) => m.organizationId === orgStore.currentOrganizationId)
  if (!stillValid) {
    orgStore.setCurrentOrganizationId(memberships[0]?.organizationId ?? null)
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      memberships: [],
      setAuth: ({ accessToken, refreshToken, user, memberships }) => {
        set({ accessToken, refreshToken, user, memberships })
        reconcileCurrentOrganization(memberships)
      },
      setMemberships: (memberships) => {
        set({ memberships })
        reconcileCurrentOrganization(memberships)
      },
      clear: () => set({ accessToken: null, refreshToken: null, user: null, memberships: [] }),
    }),
    { name: 'accounting-app.auth' },
  ),
)
