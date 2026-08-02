import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { statusTone } from '@/lib/statusTone'
import { formatCurrency, formatDate } from '@/lib/format'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { journalEntriesApi, TRANSACTION_TYPE_LABELS } from '@/api/journalEntries'

interface JournalEntryDetailModalProps {
  entryId: string | null
  onClose: () => void
}

export function JournalEntryDetailModal({ entryId, onClose }: JournalEntryDetailModalProps) {
  const queryClient = useQueryClient()
  const [voidReason, setVoidReason] = useState('')
  const [showVoidPrompt, setShowVoidPrompt] = useState(false)

  const { data: entry } = useQuery({
    queryKey: ['journal-entries', entryId],
    queryFn: () => journalEntriesApi.get(entryId!),
    enabled: Boolean(entryId),
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['journal-entries'] })
  }

  const postMutation = useMutation({
    mutationFn: () => journalEntriesApi.post(entryId!),
    onSuccess: () => {
      invalidate()
      toast.success('Journal entry posted')
    },
    onError: (error) =>
      toast.error('Could not post entry', error instanceof ApiError ? error.message : undefined),
  })

  const voidMutation = useMutation({
    mutationFn: () => journalEntriesApi.void(entryId!, voidReason),
    onSuccess: () => {
      invalidate()
      toast.success('Journal entry voided')
      setShowVoidPrompt(false)
      onClose()
    },
    onError: (error) =>
      toast.error('Could not void entry', error instanceof ApiError ? error.message : undefined),
  })

  const reverseMutation = useMutation({
    mutationFn: () => journalEntriesApi.reverse(entryId!),
    onSuccess: () => {
      invalidate()
      toast.success('Reversing entry created')
      onClose()
    },
    onError: (error) =>
      toast.error('Could not reverse entry', error instanceof ApiError ? error.message : undefined),
  })

  if (!entryId || !entry) return null

  return (
    <Modal open={Boolean(entryId)} onClose={onClose} title={entry.entryNumber} size="lg">
      <div className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
          <div>
            <div className="text-xs uppercase text-slate-400">Date</div>
            <div>{formatDate(entry.entryDate)}</div>
          </div>
          <div>
            <div className="text-xs uppercase text-slate-400">Type</div>
            <div>{TRANSACTION_TYPE_LABELS[entry.transactionType]}</div>
          </div>
          <div>
            <div className="text-xs uppercase text-slate-400">Status</div>
            <Badge tone={statusTone(entry.status)}>{entry.status}</Badge>
          </div>
          <div>
            <div className="text-xs uppercase text-slate-400">Reference</div>
            <div>{entry.referenceNumber ?? '—'}</div>
          </div>
        </div>

        {entry.memo && <p className="text-sm text-slate-600">{entry.memo}</p>}

        <Table>
          <THead>
            <TR>
              <TH>Account</TH>
              <TH>Memo</TH>
              <TH className="text-right">Debit</TH>
              <TH className="text-right">Credit</TH>
            </TR>
          </THead>
          <TBody>
            {entry.lines.map((line) => (
              <TR key={line.id}>
                <TD>
                  {line.accountCode} — {line.accountName}
                </TD>
                <TD>{line.memo ?? '—'}</TD>
                <TD className="text-right tabular-nums">
                  {Number(line.debitAmount) > 0 ? formatCurrency(line.debitAmount, entry.currencyCode) : ''}
                </TD>
                <TD className="text-right tabular-nums">
                  {Number(line.creditAmount) > 0 ? formatCurrency(line.creditAmount, entry.currencyCode) : ''}
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>

        <div className="flex justify-end gap-6 text-sm font-medium">
          <span>Total Debit: {formatCurrency(entry.totalDebit, entry.currencyCode)}</span>
          <span>Total Credit: {formatCurrency(entry.totalCredit, entry.currencyCode)}</span>
        </div>

        {showVoidPrompt && (
          <div className="rounded-md border border-red-200 bg-red-50 p-3">
            <label htmlFor="void-reason" className="text-sm font-medium text-red-900">
              Reason for voiding
            </label>
            <textarea
              id="void-reason"
              className="mt-1 block w-full rounded-md border border-red-300 px-2 py-1.5 text-sm"
              rows={2}
              value={voidReason}
              onChange={(e) => setVoidReason(e.target.value)}
            />
            <div className="mt-2 flex justify-end gap-2">
              <Button size="sm" variant="secondary" onClick={() => setShowVoidPrompt(false)}>
                Cancel
              </Button>
              <Button
                size="sm"
                variant="danger"
                disabled={!voidReason.trim() || voidMutation.isPending}
                onClick={() => voidMutation.mutate()}
              >
                Confirm Void
              </Button>
            </div>
          </div>
        )}

        <div className="flex justify-end gap-2">
          {entry.status === 'DRAFT' && (
            <Button onClick={() => postMutation.mutate()} disabled={postMutation.isPending}>
              Post
            </Button>
          )}
          {(entry.status === 'DRAFT' || entry.status === 'POSTED') && !showVoidPrompt && (
            <Button variant="danger" onClick={() => setShowVoidPrompt(true)}>
              Void
            </Button>
          )}
          {(entry.status === 'POSTED' || entry.status === 'RECONCILED') && (
            <Button
              variant="secondary"
              onClick={() => reverseMutation.mutate()}
              disabled={reverseMutation.isPending}
            >
              Reverse
            </Button>
          )}
        </div>
      </div>
    </Modal>
  )
}
