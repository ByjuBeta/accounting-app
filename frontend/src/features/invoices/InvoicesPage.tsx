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
import { invoicesApi } from '@/api/invoices'
import { InvoiceFormModal } from './InvoiceFormModal'
import { InvoiceDetailModal } from './InvoiceDetailModal'

export function InvoicesPage() {
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['invoices', page],
    queryFn: () => invoicesApi.list(page),
  })

  return (
    <div>
      <PageHeader
        title="Invoices"
        description="Create, send, and track customer invoices."
        actions={<Button onClick={() => setCreateOpen(true)}>+ New Invoice</Button>}
      />

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title="No invoices yet"
          description="Create your first invoice to start billing customers."
          action={<Button onClick={() => setCreateOpen(true)}>+ New Invoice</Button>}
        />
      ) : (
        <>
          <Table>
            <THead>
              <TR>
                <TH>Invoice #</TH>
                <TH>Customer</TH>
                <TH>Date</TH>
                <TH>Due</TH>
                <TH className="text-right">Total</TH>
                <TH className="text-right">Balance</TH>
                <TH>Status</TH>
              </TR>
            </THead>
            <TBody>
              {data.content.map((invoice) => (
                <TR
                  key={invoice.id}
                  className="cursor-pointer"
                  onClick={() => setSelectedInvoiceId(invoice.id)}
                >
                  <TD className="font-mono text-xs text-slate-500">{invoice.invoiceNumber}</TD>
                  <TD className="font-medium text-slate-900">{invoice.customerName}</TD>
                  <TD>{formatDate(invoice.invoiceDate)}</TD>
                  <TD>{formatDate(invoice.dueDate)}</TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(invoice.total, invoice.currencyCode)}
                  </TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(invoice.balanceDue, invoice.currencyCode)}
                  </TD>
                  <TD>
                    <Badge tone={statusTone(invoice.effectiveStatus)}>{invoice.effectiveStatus}</Badge>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
          <Pagination page={data.number} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}

      <InvoiceFormModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <InvoiceDetailModal invoiceId={selectedInvoiceId} onClose={() => setSelectedInvoiceId(null)} />
    </div>
  )
}
