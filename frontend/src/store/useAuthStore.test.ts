import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from './useAuthStore'
import { useOrgStore } from './useOrgStore'
import type { MembershipDto } from '@/api/auth'

function membership(organizationId: string): MembershipDto {
  return { organizationId, organizationName: `Org ${organizationId}`, role: 'ADMIN' }
}

const user = { id: 'u1', email: 'a@b.com', firstName: 'A', lastName: 'B' }

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, refreshToken: null, user: null, memberships: [] })
  useOrgStore.setState({ currentOrganizationId: null })
})

describe('useAuthStore', () => {
  it('picks the first membership when there is no current organization selected', () => {
    useAuthStore.getState().setAuth({
      accessToken: 'a',
      refreshToken: 'r',
      user,
      memberships: [membership('org-1'), membership('org-2')],
    })
    expect(useOrgStore.getState().currentOrganizationId).toBe('org-1')
  })

  it('keeps the current organization selection if still a member', () => {
    useOrgStore.setState({ currentOrganizationId: 'org-2' })
    useAuthStore.getState().setAuth({
      accessToken: 'a',
      refreshToken: 'r',
      user,
      memberships: [membership('org-1'), membership('org-2')],
    })
    expect(useOrgStore.getState().currentOrganizationId).toBe('org-2')
  })

  it('falls back to the first membership when the selected organization is no longer valid', () => {
    useOrgStore.setState({ currentOrganizationId: 'stale-org' })
    useAuthStore.getState().setMemberships([membership('org-1')])
    expect(useOrgStore.getState().currentOrganizationId).toBe('org-1')
  })

  it('clears the organization selection when there are no memberships at all', () => {
    useOrgStore.setState({ currentOrganizationId: 'org-1' })
    useAuthStore.getState().setMemberships([])
    expect(useOrgStore.getState().currentOrganizationId).toBeNull()
  })

  it('resets all auth state on clear()', () => {
    useAuthStore
      .getState()
      .setAuth({ accessToken: 'a', refreshToken: 'r', user, memberships: [membership('org-1')] })
    useAuthStore.getState().clear()
    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(useAuthStore.getState().refreshToken).toBeNull()
    expect(useAuthStore.getState().user).toBeNull()
    expect(useAuthStore.getState().memberships).toEqual([])
  })
})
