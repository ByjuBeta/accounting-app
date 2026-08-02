import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

interface OrgState {
  currentOrganizationId: string | null
  setCurrentOrganizationId: (id: string | null) => void
}

export const useOrgStore = create<OrgState>()(
  persist(
    (set) => ({
      currentOrganizationId: null,
      setCurrentOrganizationId: (id) => set({ currentOrganizationId: id }),
    }),
    {
      name: 'accounting-app.current-organization',
      storage: createJSONStorage(() => window.localStorage),
    },
  ),
)
