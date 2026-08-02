import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Select } from '@/components/ui/Select'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { statusTone } from '@/lib/statusTone'
import { formatCurrency, formatDate } from '@/lib/format'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { bankTransactionsApi, type BankTransaction, type BankTransactionStatus } from '@/api/banking'
import { BankImportModal } from './BankImportModal'
import { BankTransactionMatchModal } from './BankTransactionMatchModal'

interface TransactionsPanelProps {
  accountId: string
}

export function TransactionsPanel({ accountId }: TransactionsPanelProps) {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<BankTransactionStatus | ''>('')
  const [importOpen, setImportOpen] = useState(false)
  const [matchingTransaction, setMatchingTransaction] = useState<BankTransaction | null>(null)

  const { data: transactions, isLoading } = useQuery({
    queryKey: ['bank-transactions', accountId, statusFilter],
    queryFn: () => bankTransactionsApi.list(accountId, statusFilter || undefined),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['bank-transactions', accountId] })

  const autoMatchMutation = useMutation({
    mutationFn: () => bankTransactionsApi.autoMatch(accountId),
    onSuccess: (result) => {
      invalidate()
      toast.success('Auto-match complete', `${result.matched} transaction(s) matched`)
    },
    onError: (error) =>
      toast.error('Could not auto-match', error instanceof ApiError ? error.message : undefined),
  })

  const unmatchMutation = useMutation({
    mutationFn: (id: string) => bankTransactionsApi.unmatch(id),
    onSuccess: () => {
      invalidate()
      toast.success('Transaction unmatched')
    },
    onError: (error) =>
      toast.error('Could not unmatch', error instanceof ApiError ? error.message : undefined),
  })

  const ignoreMutation = useMutation({
    mutationFn: (id: string) => bankTransactionsApi.ignore(id),
    onSuccess: () => {
      invalidate()
      toast.success('Transaction ignored')
    },
    onError: (error) =>
      toast.error('Could not ignore', error instanceof ApiError ? error.message : undefined),
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <Select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as BankTransactionStatus | '')}
          className="w-48"
        >
          <option value="">All statuses</option>
          <option value="UNMATCHED">Unmatched</option>
          <option value="MATCHED">Matched</option>
          <option value="IGNORED">Ignored</option>
        </Select>
        <div className="flex gap-2">
          <Button
            variant="secondary"
            onClick={() => autoMatchMutation.mutate()}
            disabled={autoMatchMutation.isPending}
          >
            Auto-Match
          </Button>
          <Button onClick={() => setImportOpen(true)}>Import Transactions</Button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : !transactions || transactions.length === 0 ? (
        <EmptyState
          title="No transactions yet"
          description="Import a CSV or OFX bank feed to get started."
          action={<Button onClick={() => setImportOpen(true)}>Import Transactions</Button>}
        />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH>Date</TH>
              <TH>Description</TH>
              <TH>Check #</TH>
              <TH className="text-right">Amount</TH>
              <TH>Status</TH>
              <TH className="text-right">Actions</TH>
            </TR>
          </THead>
          <TBody>
            {transactions.map((transaction) => (
              <TR key={transaction.id}>
                <TD>{formatDate(transaction.transactionDate)}</TD>
                <TD className="font-medium text-slate-900">{transaction.description}</TD>
                <TD>{transaction.checkNumber ?? '—'}</TD>
                <TD className="text-right tabular-nums">{formatCurrency(transaction.amount)}</TD>
                <TD>
                  <Badge tone={statusTone(transaction.status)}>{transaction.status}</Badge>
                </TD>
                <TD className="text-right">
                  {transaction.status === 'UNMATCHED' && (
                    <div className="flex justify-end gap-2">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => setMatchingTransaction(transaction)}
                      >
                        Find Match
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => ignoreMutation.mutate(transaction.id)}
                        disabled={ignoreMutation.isPending}
                      >
                        Ignore
                      </Button>
                    </div>
                  )}
                  {transaction.status === 'MATCHED' && (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => unmatchMutation.mutate(transaction.id)}
                      disabled={unmatchMutation.isPending}
                    >
                      Unmatch
                    </Button>
                  )}
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      )}

      <BankImportModal open={importOpen} accountId={accountId} onClose={() => setImportOpen(false)} />
      <BankTransactionMatchModal
        transaction={matchingTransaction}
        onClose={() => setMatchingTransaction(null)}
      />
    </div>
  )
}
