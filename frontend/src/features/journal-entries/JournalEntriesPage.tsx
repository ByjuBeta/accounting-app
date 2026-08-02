import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { Pagination } from '@/components/ui/Pagination'
import { statusTone } from '@/lib/statusTone'
import { formatCurrency, formatDate } from '@/lib/format'
import { journalEntriesApi, TRANSACTION_TYPE_LABELS } from '@/api/journalEntries'
import { JournalEntryFormModal } from './JournalEntryFormModal'
import { JournalEntryDetailModal } from './JournalEntryDetailModal'

export function JournalEntriesPage() {
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const [selectedEntryId, setSelectedEntryId] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['journal-entries', page],
    queryFn: () => journalEntriesApi.list(page),
  })

  return (
    <div>
      <PageHeader
        title="Journal Entries"
        description="Double-entry transaction register."
        actions={<Button onClick={() => setCreateOpen(true)}>+ New Entry</Button>}
      />

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="No journal entries yet"
          description="Record your first double-entry transaction."
          action={<Button onClick={() => setCreateOpen(true)}>+ New Entry</Button>}
        />
      ) : (
        <>
          <Table>
            <THead>
              <TR>
                <TH>Entry #</TH>
                <TH>Date</TH>
                <TH>Type</TH>
                <TH>Memo</TH>
                <TH className="text-right">Amount</TH>
                <TH>Status</TH>
              </TR>
            </THead>
            <TBody>
              {data.content.map((entry) => (
                <TR key={entry.id} className="cursor-pointer" onClick={() => setSelectedEntryId(entry.id)}>
                  <TD className="font-mono text-xs text-slate-500">{entry.entryNumber}</TD>
                  <TD>{formatDate(entry.entryDate)}</TD>
                  <TD>{TRANSACTION_TYPE_LABELS[entry.transactionType]}</TD>
                  <TD className="max-w-xs truncate">{entry.memo ?? '—'}</TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(entry.totalDebit, entry.currencyCode)}
                  </TD>
                  <TD>
                    <Badge tone={statusTone(entry.status)}>{entry.status}</Badge>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
          <Pagination page={data.number} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}

      <JournalEntryFormModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <JournalEntryDetailModal entryId={selectedEntryId} onClose={() => setSelectedEntryId(null)} />
    </div>
  )
}
