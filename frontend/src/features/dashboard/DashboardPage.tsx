import { useOrgStore } from '@/store/useOrgStore'
import { PageHeader } from '@/components/layout/PageHeader'

export function DashboardPage() {
  const currentOrganizationId = useOrgStore((s) => s.currentOrganizationId)

  return (
    <div>
      <PageHeader title="Dashboard" description="Cash balance, payables, receivables, and recent activity." />
      {!currentOrganizationId ? (
        <p className="text-sm text-slate-500">
          Create or select a company using the switcher in the top-right corner to get started.
        </p>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {['Cash Balance', 'Accounts Receivable', 'Accounts Payable', 'Net Income (MTD)'].map((label) => (
            <div key={label} className="rounded-lg border border-slate-200 bg-white p-4">
              <div className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</div>
              <div className="mt-2 text-2xl font-semibold tabular-nums text-slate-900">—</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
