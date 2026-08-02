import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/layout/PageHeader'
import { Select } from '@/components/ui/Select'
import { EmptyState } from '@/components/ui/EmptyState'
import { cn } from '@/lib/cn'
import { accountsApi } from '@/api/accounts'
import { TransactionsPanel } from './TransactionsPanel'
import { ReconciliationPanel } from './ReconciliationPanel'

type Tab = 'transactions' | 'reconciliation'

export function BankingPage() {
  const [selectedAccountId, setSelectedAccountId] = useState('')
  const [tab, setTab] = useState<Tab>('transactions')

  const { data: accounts, isLoading } = useQuery({
    queryKey: ['accounts', 'ACTIVE'],
    queryFn: () => accountsApi.list('ACTIVE'),
  })
  const bankAccounts = (accounts ?? []).filter((a) => a.accountType === 'BANK' || a.accountType === 'CASH')
  const accountId = selectedAccountId || bankAccounts[0]?.id || ''

  return (
    <div>
      <PageHeader title="Banking" description="Bank feeds, transaction matching, and reconciliation." />

      {!isLoading && bankAccounts.length === 0 ? (
        <EmptyState
          title="No bank or cash accounts yet"
          description="Add a Bank or Cash account in the Chart of Accounts before importing transactions."
        />
      ) : (
        <div className="flex flex-col gap-5">
          <Select value={accountId} onChange={(e) => setSelectedAccountId(e.target.value)} className="w-72">
            {bankAccounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.code} — {a.name}
              </option>
            ))}
          </Select>

          {accountId && (
            <>
              <div className="flex gap-1 border-b border-slate-200">
                <TabButton active={tab === 'transactions'} onClick={() => setTab('transactions')}>
                  Transactions
                </TabButton>
                <TabButton active={tab === 'reconciliation'} onClick={() => setTab('reconciliation')}>
                  Reconciliation
                </TabButton>
              </div>

              {tab === 'transactions' ? (
                <TransactionsPanel accountId={accountId} />
              ) : (
                <ReconciliationPanel accountId={accountId} />
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        '-mb-px border-b-2 px-3 py-2 text-sm font-medium',
        active ? 'border-brand-600 text-brand-700' : 'border-transparent text-slate-500 hover:text-slate-700',
      )}
    >
      {children}
    </button>
  )
}
