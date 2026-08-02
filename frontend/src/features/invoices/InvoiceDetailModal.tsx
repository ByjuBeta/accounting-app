import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { statusTone } from '@/lib/statusTone'
import { formatCurrency, formatDate } from '@/lib/format'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { invoicesApi } from '@/api/invoices'
import { PaymentFormModal } from './PaymentFormModal'

interface InvoiceDetailModalProps {
  invoiceId: string | null
  onClose: () => void
}

export function InvoiceDetailModal({ invoiceId, onClose }: InvoiceDetailModalProps) {
  const queryClient = useQueryClient()
  const [showPaymentModal, setShowPaymentModal] = useState(false)

  const { data: invoice } = useQuery({
    queryKey: ['invoices', invoiceId],
    queryFn: () => invoicesApi.get(invoiceId!),
    enabled: Boolean(invoiceId),
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['invoices'] })
  }

  const sendMutation = useMutation({
    mutationFn: () => invoicesApi.send(invoiceId!),
    onSuccess: () => {
      invalidate()
      toast.success('Invoice sent')
    },
    onError: (error) =>
      toast.error('Could not send invoice', error instanceof ApiError ? error.message : undefined),
  })

  const cancelMutation = useMutation({
    mutationFn: () => invoicesApi.cancel(invoiceId!),
    onSuccess: () => {
      invalidate()
      toast.success('Invoice cancelled')
      onClose()
    },
    onError: (error) =>
      toast.error('Could not cancel invoice', error instanceof ApiError ? error.message : undefined),
  })

  if (!invoiceId || !invoice) return null

  return (
    <>
      <Modal open={Boolean(invoiceId)} onClose={onClose} title={invoice.invoiceNumber} size="lg">
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
            <div>
              <div className="text-xs uppercase text-slate-400">Customer</div>
              <div>{invoice.customerName}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Invoice Date</div>
              <div>{formatDate(invoice.invoiceDate)}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Due Date</div>
              <div>{formatDate(invoice.dueDate)}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Status</div>
              <Badge tone={statusTone(invoice.effectiveStatus)}>{invoice.effectiveStatus}</Badge>
            </div>
          </div>

          {invoice.memo && <p className="text-sm text-slate-600">{invoice.memo}</p>}

          <Table>
            <THead>
              <TR>
                <TH>Description</TH>
                <TH className="text-right">Qty</TH>
                <TH className="text-right">Rate</TH>
                <TH className="text-right">Tax</TH>
                <TH className="text-right">Total</TH>
              </TR>
            </THead>
            <TBody>
              {invoice.lines.map((line) => (
                <TR key={line.id}>
                  <TD>{line.description}</TD>
                  <TD className="text-right tabular-nums">{line.quantity}</TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(line.unitPrice, invoice.currencyCode)}
                  </TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(line.lineTax, invoice.currencyCode)}
                  </TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(line.lineTotal, invoice.currencyCode)}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>

          <div className="flex flex-col items-end gap-1 text-sm">
            <span>Subtotal: {formatCurrency(invoice.subtotal, invoice.currencyCode)}</span>
            <span>Tax: {formatCurrency(invoice.taxTotal, invoice.currencyCode)}</span>
            <span className="font-semibold">
              Total: {formatCurrency(invoice.total, invoice.currencyCode)}
            </span>
            <span>Paid: {formatCurrency(invoice.amountPaid, invoice.currencyCode)}</span>
            <span className="font-semibold">
              Balance Due: {formatCurrency(invoice.balanceDue, invoice.currencyCode)}
            </span>
          </div>

          <div className="flex justify-end gap-2">
            {invoice.status === 'DRAFT' && (
              <Button onClick={() => sendMutation.mutate()} disabled={sendMutation.isPending}>
                Send
              </Button>
            )}
            {(invoice.status === 'SENT' || invoice.status === 'PARTIALLY_PAID') && (
              <Button onClick={() => setShowPaymentModal(true)}>Record Payment</Button>
            )}
            {(invoice.status === 'DRAFT' || invoice.status === 'SENT') &&
              Number(invoice.amountPaid) === 0 && (
                <Button
                  variant="danger"
                  onClick={() => cancelMutation.mutate()}
                  disabled={cancelMutation.isPending}
                >
                  Cancel
                </Button>
              )}
          </div>
        </div>
      </Modal>

      <PaymentFormModal
        invoice={showPaymentModal ? invoice : null}
        onClose={() => setShowPaymentModal(false)}
      />
    </>
  )
}
