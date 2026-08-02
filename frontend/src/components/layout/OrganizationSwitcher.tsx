import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { organizationsApi } from '@/api/organizations'
import { authApi } from '@/api/auth'
import { useOrgStore } from '@/store/useOrgStore'
import { useAuthStore } from '@/store/useAuthStore'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

export function OrganizationSwitcher() {
  const { currentOrganizationId, setCurrentOrganizationId } = useOrgStore()
  const memberships = useAuthStore((s) => s.memberships)
  const setMemberships = useAuthStore((s) => s.setMemberships)
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')

  const createOrganization = useMutation({
    mutationFn: organizationsApi.create,
    onSuccess: async (organization) => {
      const me = await authApi.me()
      setMemberships(me.memberships)
      setCurrentOrganizationId(organization.id)
      setCreating(false)
      setName('')
    },
  })

  if (creating || memberships.length === 0) {
    return (
      <form
        className="flex items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          if (!name.trim()) return
          createOrganization.mutate({
            name: name.trim(),
            baseCurrencyCode: 'USD',
            timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
          })
        }}
      >
        <Input
          autoFocus
          placeholder="Company name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-48"
        />
        <Button type="submit" size="sm" disabled={createOrganization.isPending}>
          Create
        </Button>
        {memberships.length > 0 && (
          <Button type="button" variant="ghost" size="sm" onClick={() => setCreating(false)}>
            Cancel
          </Button>
        )}
      </form>
    )
  }

  return (
    <div className="flex items-center gap-2">
      <select
        className="rounded-md border border-slate-300 bg-white px-2.5 py-1.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500"
        value={currentOrganizationId ?? ''}
        onChange={(e) => setCurrentOrganizationId(e.target.value)}
      >
        {memberships.map((membership) => (
          <option key={membership.organizationId} value={membership.organizationId}>
            {membership.organizationName}
          </option>
        ))}
      </select>
      <Button type="button" variant="ghost" size="sm" onClick={() => setCreating(true)}>
        + New
      </Button>
    </div>
  )
}
