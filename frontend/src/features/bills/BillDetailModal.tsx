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
import { billsApi } from '@/api/bills'
import { BillPaymentFormModal } from './BillPaymentFormModal'

interface BillDetailModalProps {
  billId: string | null
  onClose: () => void
}

export function BillDetailModal({ billId, onClose }: BillDetailModalProps) {
  const queryClient = useQueryClient()
  const [showPaymentModal, setShowPaymentModal] = useState(false)

  const { data: bill } = useQuery({
    queryKey: ['bills', billId],
    queryFn: () => billsApi.get(billId!),
    enabled: Boolean(billId),
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['bills'] })
  }

  const receiveMutation = useMutation({
    mutationFn: () => billsApi.receive(billId!),
    onSuccess: () => {
      invalidate()
      toast.success('Bill received')
    },
    onError: (error) =>
      toast.error('Could not receive bill', error instanceof ApiError ? error.message : undefined),
  })

  const cancelMutation = useMutation({
    mutationFn: () => billsApi.cancel(billId!),
    onSuccess: () => {
      invalidate()
      toast.success('Bill cancelled')
      onClose()
    },
    onError: (error) =>
      toast.error('Could not cancel bill', error instanceof ApiError ? error.message : undefined),
  })

  if (!billId || !bill) return null

  return (
    <>
      <Modal open={Boolean(billId)} onClose={onClose} title={bill.billNumber} size="lg">
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
            <div>
              <div className="text-xs uppercase text-slate-400">Vendor</div>
              <div>{bill.vendorName}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Bill Date</div>
              <div>{formatDate(bill.billDate)}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Due Date</div>
              <div>{formatDate(bill.dueDate)}</div>
            </div>
            <div>
              <div className="text-xs uppercase text-slate-400">Status</div>
              <Badge tone={statusTone(bill.effectiveStatus)}>{bill.effectiveStatus}</Badge>
            </div>
          </div>

          {bill.vendorReferenceNumber && (
            <p className="text-sm text-slate-500">Vendor Ref #: {bill.vendorReferenceNumber}</p>
          )}
          {bill.memo && <p className="text-sm text-slate-600">{bill.memo}</p>}

          {bill.eligibleForEarlyPaymentDiscountToday && Number(bill.earlyPaymentDiscountAmount) > 0 && (
            <p className="rounded-md bg-emerald-50 px-3 py-2 text-xs text-emerald-800">
              Eligible for an early-payment discount of{' '}
              <strong className="tabular-nums">
                {formatCurrency(bill.earlyPaymentDiscountAmount, bill.currencyCode)}
              </strong>{' '}
              if paid today.
            </p>
          )}

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
              {bill.lines.map((line) => (
                <TR key={line.id}>
                  <TD>{line.description}</TD>
                  <TD className="text-right tabular-nums">{line.quantity}</TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(line.unitPrice, bill.currencyCode)}
                  </TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(line.lineTax, bill.currencyCode)}
                  </TD>
                  <TD className="text-right tabular-nums">
                    {formatCurrency(line.lineTotal, bill.currencyCode)}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>

          <div className="flex flex-col items-end gap-1 text-sm">
            <span>Subtotal: {formatCurrency(bill.subtotal, bill.currencyCode)}</span>
            <span>Tax: {formatCurrency(bill.taxTotal, bill.currencyCode)}</span>
            <span className="font-semibold">Total: {formatCurrency(bill.total, bill.currencyCode)}</span>
            <span>Paid: {formatCurrency(bill.amountPaid, bill.currencyCode)}</span>
            <span className="font-semibold">
              Balance Due: {formatCurrency(bill.balanceDue, bill.currencyCode)}
            </span>
          </div>

          <div className="flex justify-end gap-2">
            {bill.status === 'DRAFT' && (
              <Button onClick={() => receiveMutation.mutate()} disabled={receiveMutation.isPending}>
                Receive
              </Button>
            )}
            {(bill.status === 'RECEIVED' || bill.status === 'PARTIALLY_PAID') && (
              <Button onClick={() => setShowPaymentModal(true)}>Pay Bill</Button>
            )}
            {(bill.status === 'DRAFT' || bill.status === 'RECEIVED') && Number(bill.amountPaid) === 0 && (
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

      <BillPaymentFormModal
        bill={showPaymentModal ? bill : null}
        onClose={() => setShowPaymentModal(false)}
      />
    </>
  )
}
