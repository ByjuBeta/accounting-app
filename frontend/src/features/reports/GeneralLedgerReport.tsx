import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Input } from '@/components/ui/Input'
import { Field } from '@/components/ui/Field'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { AccountPicker } from '@/components/domain/AccountPicker'
import { formatCurrency, formatDate } from '@/lib/format'
import { accountsApi } from '@/api/accounts'
import { reportsApi } from '@/api/reports'

function firstOfMonth() {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10)
}

export function GeneralLedgerReport() {
  const [accountId, setAccountId] = useState('')
  const [fromDate, setFromDate] = useState(firstOfMonth())
  const [toDate, setToDate] = useState(new Date().toISOString().slice(0, 10))

  const { data: accounts } = useQuery({
    queryKey: ['accounts', 'ACTIVE'],
    queryFn: () => accountsApi.list('ACTIVE'),
  })

  const { data, isLoading } = useQuery({
    queryKey: ['reports', 'general-ledger', accountId, fromDate, toDate],
    queryFn: () => reportsApi.generalLedger(accountId, fromDate, toDate),
    enabled: Boolean(accountId),
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-4">
        <Field label="Account" htmlFor="accountId" className="w-64">
          <AccountPicker
            id="accountId"
            accounts={accounts ?? []}
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
          />
        </Field>
        <Field label="From" htmlFor="fromDate" className="w-48">
          <Input id="fromDate" type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} />
        </Field>
        <Field label="To" htmlFor="toDate" className="w-48">
          <Input id="toDate" type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} />
        </Field>
      </div>

      {!accountId ? (
        <EmptyState
          title="Select an account"
          description="Choose an account to view its general ledger detail."
        />
      ) : isLoading || !data ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <h3 className="text-sm font-semibold text-slate-900">
            {data.accountCode} — {data.accountName}
          </h3>
          <Table>
            <THead>
              <TR>
                <TH>Entry #</TH>
                <TH>Date</TH>
                <TH>Memo</TH>
                <TH className="text-right">Debit</TH>
                <TH className="text-right">Credit</TH>
                <TH className="text-right">Running Balance</TH>
              </TR>
            </THead>
            <TBody>
              <TR key="beginning">
                <TD colSpan={5} className="font-medium">
                  Beginning Balance
                </TD>
                <TD className="text-right tabular-nums font-medium">
                  {formatCurrency(data.beginningBalance)}
                </TD>
              </TR>
              {data.lines.map((line) => (
                <TR key={line.journalEntryLineId}>
                  <TD className="font-mono text-xs text-slate-500">{line.entryNumber}</TD>
                  <TD>{formatDate(line.entryDate)}</TD>
                  <TD>{line.memo ?? '—'}</TD>
                  <TD className="text-right tabular-nums">
                    {Number(line.debit) > 0 ? formatCurrency(line.debit) : '—'}
                  </TD>
                  <TD className="text-right tabular-nums">
                    {Number(line.credit) > 0 ? formatCurrency(line.credit) : '—'}
                  </TD>
                  <TD className="text-right tabular-nums">{formatCurrency(line.runningBalance)}</TD>
                </TR>
              ))}
              <TR key="ending" className="font-semibold">
                <TD colSpan={5}>Ending Balance</TD>
                <TD className="text-right tabular-nums">{formatCurrency(data.endingBalance)}</TD>
              </TR>
            </TBody>
          </Table>
        </div>
      )}
    </div>
  )
}
