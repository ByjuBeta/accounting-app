import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { statusTone } from '@/lib/statusTone'
import { formatCurrency, formatDate } from '@/lib/format'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { reconciliationsApi } from '@/api/banking'
import { StartReconciliationModal } from './StartReconciliationModal'

interface ReconciliationPanelProps {
  accountId: string
}

export function ReconciliationPanel({ accountId }: ReconciliationPanelProps) {
  const queryClient = useQueryClient()
  const [startOpen, setStartOpen] = useState(false)

  const { data: history, isLoading } = useQuery({
    queryKey: ['reconciliations', accountId],
    queryFn: () => reconciliationsApi.history(accountId),
  })

  const inProgress = history?.find((r) => r.status === 'IN_PROGRESS') ?? null

  const { data: worksheet } = useQuery({
    queryKey: ['reconciliations', inProgress?.id, 'worksheet'],
    queryFn: () => reconciliationsApi.worksheet(inProgress!.id),
    enabled: Boolean(inProgress),
  })

  const completeMutation = useMutation({
    mutationFn: () => reconciliationsApi.complete(inProgress!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reconciliations', accountId] })
      queryClient.invalidateQueries({ queryKey: ['bank-transactions', accountId] })
      toast.success('Reconciliation completed')
    },
    onError: (error) =>
      toast.error('Could not complete reconciliation', error instanceof ApiError ? error.message : undefined),
  })

  if (isLoading) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      {inProgress && worksheet ? (
        <div className="rounded-lg border border-slate-200 bg-white p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-slate-900">
                Reconciliation — statement date {formatDate(worksheet.statementDate)}
              </h3>
              <p className="text-xs text-slate-500">
                {worksheet.clearedTransactionCount} cleared transaction(s)
              </p>
            </div>
            <Badge tone={Number(worksheet.difference) === 0 ? 'green' : 'amber'}>
              {Number(worksheet.difference) === 0
                ? 'Balanced'
                : `Off by ${formatCurrency(worksheet.difference)}`}
            </Badge>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
            <div>
              <div className="text-xs uppercase text-slate-400">Beginning Balance</div>
              <div className="tabular-nums">{formatCurrency(worksheet.beginningBalance)}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Statement Ending</div>
              <div className="tabular-nums">{formatCurrency(worksheet.statementEndingBalance)}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Cleared Total</div>
              <div className="tabular-nums">{formatCurrency(worksheet.clearedTotal)}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Projected Ending</div>
              <div className="tabular-nums">{formatCurrency(worksheet.projectedEndingBalance)}</div>
            </div>
          </div>

          {worksheet.outstandingLines.length > 0 && (
            <div className="mt-4">
              <h4 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
                Outstanding Ledger Lines
              </h4>
              <Table>
                <THead>
                  <TR>
                    <TH>Entry #</TH>
                    <TH>Date</TH>
                    <TH>Description</TH>
                    <TH className="text-right">Amount</TH>
                  </TR>
                </THead>
                <TBody>
                  {worksheet.outstandingLines.map((line) => (
                    <TR key={line.journalEntryLineId}>
                      <TD className="font-mono text-xs text-slate-500">{line.entryNumber}</TD>
                      <TD>{formatDate(line.entryDate)}</TD>
                      <TD>{line.description ?? '—'}</TD>
                      <TD className="text-right tabular-nums">{formatCurrency(line.amount)}</TD>
                    </TR>
                  ))}
                </TBody>
              </Table>
            </div>
          )}

          <div className="mt-4 flex justify-end">
            <Button
              onClick={() => completeMutation.mutate()}
              disabled={Number(worksheet.difference) !== 0 || completeMutation.isPending}
            >
              Complete Reconciliation
            </Button>
          </div>
        </div>
      ) : (
        <EmptyState
          title="No reconciliation in progress"
          description="Start a new reconciliation against a bank statement to match and lock in cleared transactions."
          action={<Button onClick={() => setStartOpen(true)}>Start Reconciliation</Button>}
        />
      )}

      {history && history.length > 0 && (
        <div>
          <h4 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">History</h4>
          <Table>
            <THead>
              <TR>
                <TH>Statement Date</TH>
                <TH className="text-right">Ending Balance</TH>
                <TH className="text-right">Cleared Balance</TH>
                <TH>Status</TH>
                <TH>Completed</TH>
              </TR>
            </THead>
            <TBody>
              {history.map((reconciliation) => (
                <TR key={reconciliation.id}>
                  <TD>{formatDate(reconciliation.statementDate)}</TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(reconciliation.statementEndingBalance)}
                  </TD>
                  <TD className="text-right tabular-nums">
                    {reconciliation.clearedBalance ? formatCurrency(reconciliation.clearedBalance) : '—'}
                  </TD>
                  <TD>
                    <Badge tone={statusTone(reconciliation.status)}>{reconciliation.status}</Badge>
                  </TD>
                  <TD>{reconciliation.completedAt ? formatDate(reconciliation.completedAt) : '—'}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </div>
      )}

      <StartReconciliationModal open={startOpen} accountId={accountId} onClose={() => setStartOpen(false)} />
    </div>
  )
}
