import { PageHeader } from '@/components/layout/PageHeader'
import { EmptyState } from '@/components/ui/EmptyState'
import { useOrgStore } from '@/store/useOrgStore'
import { useAuthStore } from '@/store/useAuthStore'
import { OrganizationProfileForm } from './OrganizationProfileForm'
import { MembersPanel } from './MembersPanel'

export function SettingsPage() {
  const currentOrganizationId = useOrgStore((s) => s.currentOrganizationId)
  const memberships = useAuthStore((s) => s.memberships)
  const currentMembership = memberships.find((m) => m.organizationId === currentOrganizationId)
  const isAdmin = currentMembership?.role === 'ADMIN'

  return (
    <div>
      <PageHeader title="Settings" description="Company settings, users, and permissions." />

      {!currentOrganizationId ? (
        <EmptyState title="No organization selected" description="Create or select an organization first." />
      ) : (
        <div className="flex flex-col gap-8">
          <section>
            <h2 className="mb-3 text-sm font-semibold text-slate-900">Organization</h2>
            <OrganizationProfileForm organizationId={currentOrganizationId} canEdit={isAdmin} />
          </section>

          <section>
            <h2 className="mb-3 text-sm font-semibold text-slate-900">Members</h2>
            <MembersPanel canManage={isAdmin} />
          </section>
        </div>
      )}
    </div>
  )
}
