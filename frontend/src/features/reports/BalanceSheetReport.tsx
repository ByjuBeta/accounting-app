import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/ui/Badge'
import { Input } from '@/components/ui/Input'
import { Field } from '@/components/ui/Field'
import { Table, TBody, TR, TD } from '@/components/ui/Table'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatCurrency, formatDate } from '@/lib/format'
import { reportsApi, type BalanceSheetSection } from '@/api/reports'

function Section({ section }: { section: BalanceSheetSection }) {
  return (
    <div>
      <h3 className="mb-2 text-sm font-semibold text-slate-900">{section.label}</h3>
      <Table>
        <TBody>
          {section.lines.map((line) => (
            <TR key={line.accountId ?? line.name}>
              <TD className="text-xs text-slate-500">{line.code}</TD>
              <TD>{line.name}</TD>
              <TD className="text-right tabular-nums">{formatCurrency(line.balance)}</TD>
            </TR>
          ))}
          <TR key="total" className="font-semibold">
            <TD colSpan={2}>Total {section.label}</TD>
            <TD className="text-right tabular-nums">{formatCurrency(section.total)}</TD>
          </TR>
        </TBody>
      </Table>
    </div>
  )
}

export function BalanceSheetReport() {
  const [asOfDate, setAsOfDate] = useState(new Date().toISOString().slice(0, 10))

  const { data, isLoading } = useQuery({
    queryKey: ['reports', 'balance-sheet', asOfDate],
    queryFn: () => reportsApi.balanceSheet(asOfDate),
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-end justify-between">
        <Field label="As Of" htmlFor="asOfDate" className="w-48">
          <Input id="asOfDate" type="date" value={asOfDate} onChange={(e) => setAsOfDate(e.target.value)} />
        </Field>
        {data && (
          <Badge tone={data.balanced ? 'green' : 'red'}>
            {data.balanced ? 'Balanced' : 'Out of Balance'}
          </Badge>
        )}
      </div>

      {isLoading || !data ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          <p className="text-xs text-slate-500">As of {formatDate(data.asOfDate)}</p>
          <Section section={data.assets} />
          <Section section={data.liabilities} />
          <Section section={data.equity} />
          <Table>
            <TBody>
              <TR>
                <TD>Net Income to Date</TD>
                <TD className="text-right tabular-nums">{formatCurrency(data.netIncomeToDate)}</TD>
              </TR>
              <TR className="font-semibold">
                <TD>Total Liabilities and Equity</TD>
                <TD className="text-right tabular-nums">{formatCurrency(data.totalLiabilitiesAndEquity)}</TD>
              </TR>
            </TBody>
          </Table>
        </div>
      )}
    </div>
  )
}
