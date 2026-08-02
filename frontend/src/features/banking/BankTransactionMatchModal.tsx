import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatCurrency, formatDate } from '@/lib/format'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { bankTransactionsApi, type BankTransaction } from '@/api/banking'

interface BankTransactionMatchModalProps {
  transaction: BankTransaction | null
  onClose: () => void
}

export function BankTransactionMatchModal({ transaction, onClose }: BankTransactionMatchModalProps) {
  const queryClient = useQueryClient()

  const { data: candidates, isLoading } = useQuery({
    queryKey: ['bank-transactions', transaction?.id, 'suggested-matches'],
    queryFn: () => bankTransactionsApi.suggestedMatches(transaction!.id),
    enabled: Boolean(transaction),
  })

  const matchMutation = useMutation({
    mutationFn: (journalEntryLineId: string) =>
      bankTransactionsApi.match(transaction!.id, journalEntryLineId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bank-transactions'] })
      toast.success('Transaction matched')
      onClose()
    },
    onError: (error) => {
      toast.error('Could not match transaction', error instanceof ApiError ? error.message : undefined)
    },
  })

  if (!transaction) return null

  return (
    <Modal
      open={Boolean(transaction)}
      onClose={onClose}
      title={`Match — ${transaction.description}`}
      size="lg"
    >
      <div className="flex flex-col gap-4">
        <p className="text-sm text-slate-500">
          {formatDate(transaction.transactionDate)} · {formatCurrency(transaction.amount)}
        </p>

        {isLoading ? (
          <div className="flex flex-col gap-2">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : !candidates || candidates.length === 0 ? (
          <EmptyState
            title="No candidate ledger lines"
            description="No unreconciled journal entry lines on this account match this transaction's amount within a 5-day window."
          />
        ) : (
          <div className="flex flex-col gap-2">
            {candidates.map((line) => {
              const amount = Number(line.debitAmount) > 0 ? line.debitAmount : line.creditAmount
              return (
                <button
                  key={line.id}
                  type="button"
                  onClick={() => matchMutation.mutate(line.id)}
                  disabled={matchMutation.isPending}
                  className="flex items-center justify-between rounded-md border border-slate-200 px-3 py-2.5 text-left text-sm hover:border-brand-400 hover:bg-brand-50 disabled:opacity-50"
                >
                  <div>
                    <div className="font-medium text-slate-900">{line.memo || line.accountName}</div>
                    <div className="text-xs text-slate-500">
                      {line.accountCode} — {line.accountName}
                    </div>
                  </div>
                  <div className="tabular-nums font-medium text-slate-700">{formatCurrency(amount)}</div>
                </button>
              )
            })}
          </div>
        )}

        <div className="flex justify-end">
          <Button type="button" variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </Modal>
  )
}
