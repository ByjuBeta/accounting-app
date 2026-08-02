import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Field } from '@/components/ui/Field'
import { Table, TBody, TR, TD } from '@/components/ui/Table'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatCurrency } from '@/lib/format'
import { reportsApi, type CashFlowLine, type CashFlowMethod } from '@/api/reports'

function LineRows({ lines }: { lines: CashFlowLine[] }) {
  return (
    <>
      {lines.map((line) => (
        <TR key={line.label}>
          <TD>{line.label}</TD>
          <TD className="text-right tabular-nums">{formatCurrency(line.amount)}</TD>
        </TR>
      ))}
    </>
  )
}

function firstOfMonth() {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10)
}

export function CashFlowReport() {
  const [fromDate, setFromDate] = useState(firstOfMonth())
  const [toDate, setToDate] = useState(new Date().toISOString().slice(0, 10))
  const [method, setMethod] = useState<CashFlowMethod>('DIRECT')

  const { data, isLoading } = useQuery({
    queryKey: ['reports', 'cash-flow', fromDate, toDate, method],
    queryFn: () => reportsApi.cashFlow(fromDate, toDate, method),
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
        <Field label="Method" htmlFor="method" className="w-40">
          <Select id="method" value={method} onChange={(e) => setMethod(e.target.value as CashFlowMethod)}>
            <option value="DIRECT">Direct</option>
            <option value="INDIRECT">Indirect</option>
          </Select>
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
            <h3 className="mb-2 text-sm font-semibold text-slate-900">Operating Activities</h3>
            <Table>
              <TBody>
                <LineRows lines={data.operatingLines} />
                <TR className="font-semibold">
                  <TD>Net Cash from Operating Activities</TD>
                  <TD className="text-right tabular-nums">{formatCurrency(data.operatingTotal)}</TD>
                </TR>
              </TBody>
            </Table>
          </div>
          <div>
            <h3 className="mb-2 text-sm font-semibold text-slate-900">Investing Activities</h3>
            <Table>
              <TBody>
                <LineRows lines={data.investingLines} />
                <TR className="font-semibold">
                  <TD>Net Cash from Investing Activities</TD>
                  <TD className="text-right tabular-nums">{formatCurrency(data.investingTotal)}</TD>
                </TR>
              </TBody>
            </Table>
          </div>
          <div>
            <h3 className="mb-2 text-sm font-semibold text-slate-900">Financing Activities</h3>
            <Table>
              <TBody>
                <LineRows lines={data.financingLines} />
                <TR className="font-semibold">
                  <TD>Net Cash from Financing Activities</TD>
                  <TD className="text-right tabular-nums">{formatCurrency(data.financingTotal)}</TD>
                </TR>
              </TBody>
            </Table>
          </div>
          <Table>
            <TBody>
              <TR className="font-semibold">
                <TD>Net Change in Cash</TD>
                <TD className="text-right tabular-nums">{formatCurrency(data.netChangeInCash)}</TD>
              </TR>
              <TR>
                <TD>Beginning Cash</TD>
                <TD className="text-right tabular-nums">{formatCurrency(data.beginningCash)}</TD>
              </TR>
              <TR className="font-semibold">
                <TD>Ending Cash</TD>
                <TD className="text-right tabular-nums">{formatCurrency(data.endingCash)}</TD>
              </TR>
            </TBody>
          </Table>
        </div>
      )}
    </div>
  )
}
