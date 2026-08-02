import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { Pagination } from '@/components/ui/Pagination'
import { statusTone } from '@/lib/statusTone'
import { formatCurrency, formatDate } from '@/lib/format'
import { billsApi } from '@/api/bills'
import { BillFormModal } from './BillFormModal'
import { BillDetailModal } from './BillDetailModal'

export function BillsPage() {
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const [selectedBillId, setSelectedBillId] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['bills', page],
    queryFn: () => billsApi.list(page),
  })

  return (
    <div>
      <PageHeader
        title="Bills"
        description="Enter, receive, and pay vendor bills."
        actions={<Button onClick={() => setCreateOpen(true)}>+ New Bill</Button>}
      />

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="No bills yet"
          description="Enter your first vendor bill to start tracking payables."
          action={<Button onClick={() => setCreateOpen(true)}>+ New Bill</Button>}
        />
      ) : (
        <>
          <Table>
            <THead>
              <TR>
                <TH>Bill #</TH>
                <TH>Vendor</TH>
                <TH>Date</TH>
                <TH>Due</TH>
                <TH className="text-right">Total</TH>
                <TH className="text-right">Balance</TH>
                <TH>Status</TH>
              </TR>
            </THead>
            <TBody>
              {data.content.map((bill) => (
                <TR key={bill.id} className="cursor-pointer" onClick={() => setSelectedBillId(bill.id)}>
                  <TD className="font-mono text-xs text-slate-500">{bill.billNumber}</TD>
                  <TD className="font-medium text-slate-900">{bill.vendorName}</TD>
                  <TD>{formatDate(bill.billDate)}</TD>
                  <TD>{formatDate(bill.dueDate)}</TD>
                  <TD className="text-right tabular-nums">{formatCurrency(bill.total, bill.currencyCode)}</TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(bill.balanceDue, bill.currencyCode)}
                  </TD>
                  <TD>
                    <Badge tone={statusTone(bill.effectiveStatus)}>{bill.effectiveStatus}</Badge>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
          <Pagination page={data.number} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}

      <BillFormModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <BillDetailModal billId={selectedBillId} onClose={() => setSelectedBillId(null)} />
    </div>
  )
}
