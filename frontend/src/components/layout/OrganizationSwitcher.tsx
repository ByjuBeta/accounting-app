import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { organizationsApi } from '@/api/organizations'
import { useOrgStore } from '@/store/useOrgStore'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

export function OrganizationSwitcher() {
  const queryClient = useQueryClient()
  const { currentOrganizationId, setCurrentOrganizationId } = useOrgStore()
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')

  const { data: organizations, isLoading } = useQuery({
    queryKey: ['organizations'],
    queryFn: organizationsApi.list,
  })

  const createOrganization = useMutation({
    mutationFn: organizationsApi.create,
    onSuccess: (organization) => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      setCurrentOrganizationId(organization.id)
      setCreating(false)
      setName('')
    },
  })

  useEffect(() => {
    if (!organizations) return
    if (organizations.length === 0) {
      if (currentOrganizationId) setCurrentOrganizationId(null)
      return
    }
    const stillExists = organizations.some((o) => o.id === currentOrganizationId)
    if (!stillExists) {
      setCurrentOrganizationId(organizations[0]!.id)
    }
  }, [organizations, currentOrganizationId, setCurrentOrganizationId])

  if (isLoading) {
    return <div className="h-9 w-48 animate-pulse rounded-md bg-slate-100" />
  }

  if (creating || !organizations || organizations.length === 0) {
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
        {organizations && organizations.length > 0 && (
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
        {organizations.map((org) => (
          <option key={org.id} value={org.id}>
            {org.name}
          </option>
        ))}
      </select>
      <Button type="button" variant="ghost" size="sm" onClick={() => setCreating(true)}>
        + New
      </Button>
    </div>
  )
}
