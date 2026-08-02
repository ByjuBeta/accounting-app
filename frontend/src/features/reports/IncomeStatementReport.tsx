import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Input } from '@/components/ui/Input'
import { Field } from '@/components/ui/Field'
import { Table, TBody, TR, TD } from '@/components/ui/Table'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatCurrency } from '@/lib/format'
import { reportsApi, type AccountBalanceLine } from '@/api/reports'

function LineRows({ lines }: { lines: AccountBalanceLine[] }) {
  return (
    <>
      {lines.map((line) => (
        <TR key={line.accountId}>
          <TD className="text-xs text-slate-500">{line.code}</TD>
          <TD>{line.name}</TD>
          <TD className="text-right tabular-nums">{formatCurrency(line.balance)}</TD>
        </TR>
      ))}
    </>
  )
}

function firstOfMonth() {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10)
}

export function IncomeStatementReport() {
  const [fromDate, setFromDate] = useState(firstOfMonth())
  const [toDate, setToDate] = useState(new Date().toISOString().slice(0, 10))

  const { data, isLoading } = useQuery({
    queryKey: ['reports', 'income-statement', fromDate, toDate],
    queryFn: () => reportsApi.incomeStatement(fromDate, toDate),
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-4">
        <Field label="From" htmlFor="fromDate" className="w-48">
          <Input id="fromDate" type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} />
        </Field>
        <Field label="To" htmlFor="toDate" className="w-48">
          <Input id="toDate" type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} />
        </Field>
      </div>

      {isLoading || !data ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          <div>
            <h3 className="mb-2 text-sm font-semibold text-slate-900">Revenue</h3>
            <Table>
              <TBody>
                <LineRows lines={data.revenue} />
                <TR className="font-semibold">
                  <TD colSpan={2}>Total Revenue</TD>
                  <TD className="text-right tabular-nums">{formatCurrency(data.totalRevenue)}</TD>
                </TR>
              </TBody>
            </Table>
          </div>
          <div>
            <h3 className="mb-2 text-sm font-semibold text-slate-900">Expenses</h3>
            <Table>
              <TBody>
                <LineRows lines={data.expenses} />
                <TR className="font-semibold">
                  <TD colSpan={2}>Total Expenses</TD>
                  <TD className="text-right tabular-nums">{formatCurrency(data.totalExpense)}</TD>
                </TR>
              </TBody>
            </Table>
          </div>
          <Table>
            <TBody>
              <TR className="font-semibold">
                <TD>Net Income</TD>
                <TD className="text-right tabular-nums">{formatCurrency(data.netIncome)}</TD>
              </TR>
            </TBody>
          </Table>
        </div>
      )}
    </div>
  )
}
