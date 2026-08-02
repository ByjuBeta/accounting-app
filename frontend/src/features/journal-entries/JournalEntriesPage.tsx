import { PageHeader } from '@/components/layout/PageHeader'

export function JournalEntriesPage() {
  return (
    <div>
      <PageHeader title="Journal Entries" description="Double-entry transaction register." />
      <p className="text-sm text-slate-500">Coming next: entry list and the double-entry creation form.</p>
    </div>
  )
}
