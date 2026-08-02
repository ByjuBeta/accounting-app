import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
import { Select } from '@/components/ui/Select'
import { Badge } from '@/components/ui/Badge'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { statusTone } from '@/lib/statusTone'
import { accountsApi, ACCOUNT_TYPE_LABELS, type Account, type AccountStatus } from '@/api/accounts'
import { buildAccountTree } from './accountTree'
import { AccountFormModal } from './AccountFormModal'

export function ChartOfAccountsPage() {
  const [statusFilter, setStatusFilter] = useState<AccountStatus | ''>('ACTIVE')
  const [modalOpen, setModalOpen] = useState(false)
  const [editingAccount, setEditingAccount] = useState<Account | null>(null)

  const { data: accounts, isLoading } = useQuery({
    queryKey: ['accounts', statusFilter],
    queryFn: () => accountsApi.list(statusFilter || undefined),
  })

  const tree = accounts ? buildAccountTree(accounts) : []

  function openCreateModal() {
    setEditingAccount(null)
    setModalOpen(true)
  }

  function openEditModal(account: Account) {
    setEditingAccount(account)
    setModalOpen(true)
  }

  return (
    <div>
      <PageHeader
        title="Chart of Accounts"
        description="Your organization's account hierarchy."
        actions={<Button onClick={openCreateModal}>+ Add Account</Button>}
      />

      <div className="mb-3 flex items-center gap-2">
        <label htmlFor="status-filter" className="text-sm text-slate-500">
          Status
        </label>
        <Select
          id="status-filter"
          className="w-40"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as AccountStatus | '')}
        >
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
          <option value="ARCHIVED">Archived</option>
          <option value="">All</option>
        </Select>
      </div>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : tree.length === 0 ? (
        <EmptyState
          title="No accounts yet"
          description="Create your first account to start building your chart of accounts."
          action={<Button onClick={openCreateModal}>+ Add Account</Button>}
        />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH>Code</TH>
              <TH>Name</TH>
              <TH>Type</TH>
              <TH>Currency</TH>
              <TH>Status</TH>
            </TR>
          </THead>
          <TBody>
            {tree.map((account) => (
              <TR key={account.id} className="cursor-pointer" onClick={() => openEditModal(account)}>
                <TD className="font-mono text-xs text-slate-500">{account.code}</TD>
                <TD>
                  <span
                    style={{ paddingLeft: `${account.depth * 1.25}rem` }}
                    className="font-medium text-slate-900"
                  >
                    {account.name}
                  </span>
                </TD>
                <TD>{ACCOUNT_TYPE_LABELS[account.accountType]}</TD>
                <TD>{account.currencyCode}</TD>
                <TD>
                  <Badge tone={statusTone(account.status)}>{account.status}</Badge>
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      )}

      <AccountFormModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        account={editingAccount}
        allAccounts={accounts ?? []}
      />
    </div>
  )
}
