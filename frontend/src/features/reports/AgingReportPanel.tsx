import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Input } from '@/components/ui/Input'
import { Field } from '@/components/ui/Field'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatCurrency } from '@/lib/format'
import type { AgingBucket } from '@/api/arAging'

interface AgingRow {
  name: string
  bucket: AgingBucket
}

interface AgingReportPanelProps {
  queryKey: string
  fetcher: (asOfDate?: string) => Promise<{ asOfDate: string; rows: unknown[]; totals: AgingBucket }>
  toRows: (rows: unknown[]) => AgingRow[]
  nameLabel: string
  emptyTitle: string
}

export function AgingReportPanel({
  queryKey,
  fetcher,
  toRows,
  nameLabel,
  emptyTitle,
}: AgingReportPanelProps) {
  const [asOfDate, setAsOfDate] = useState(new Date().toISOString().slice(0, 10))

  const { data, isLoading } = useQuery({
    queryKey: ['reports', queryKey, asOfDate],
    queryFn: () => fetcher(asOfDate),
  })

  const rows = data ? toRows(data.rows) : []

  return (
    <div className="flex flex-col gap-4">
      <Field label="As Of" htmlFor="asOfDate" className="w-48">
        <Input id="asOfDate" type="date" value={asOfDate} onChange={(e) => setAsOfDate(e.target.value)} />
      </Field>

      {isLoading || !data ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : rows.length === 0 ? (
        <EmptyState title={emptyTitle} />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH>{nameLabel}</TH>
              <TH className="text-right">Current</TH>
              <TH className="text-right">1-30</TH>
              <TH className="text-right">31-60</TH>
              <TH className="text-right">61-90</TH>
              <TH className="text-right">90+</TH>
            </TR>
          </THead>
          <TBody>
            {rows.map((row) => (
              <TR key={row.name}>
                <TD className="font-medium text-slate-900">{row.name}</TD>
                <TD className="text-right tabular-nums">{formatCurrency(row.bucket.current)}</TD>
                <TD className="text-right tabular-nums">{formatCurrency(row.bucket.days1to30)}</TD>
                <TD className="text-right tabular-nums">{formatCurrency(row.bucket.days31to60)}</TD>
                <TD className="text-right tabular-nums">{formatCurrency(row.bucket.days61to90)}</TD>
                <TD className="text-right tabular-nums">{formatCurrency(row.bucket.over90)}</TD>
              </TR>
            ))}
            <TR key="total" className="font-semibold">
              <TD>Total</TD>
              <TD className="text-right tabular-nums">{formatCurrency(data.totals.current)}</TD>
              <TD className="text-right tabular-nums">{formatCurrency(data.totals.days1to30)}</TD>
              <TD className="text-right tabular-nums">{formatCurrency(data.totals.days31to60)}</TD>
              <TD className="text-right tabular-nums">{formatCurrency(data.totals.days61to90)}</TD>
              <TD className="text-right tabular-nums">{formatCurrency(data.totals.over90)}</TD>
            </TR>
          </TBody>
        </Table>
      )}
    </div>
  )
}
