import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Field } from '@/components/ui/Field'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { accountsApi } from '@/api/accounts'
import { billPaymentsApi } from '@/api/billPayments'
import type { Bill } from '@/api/bills'

const formSchema = z.object({
  paymentDate: z.string().min(1, 'Required'),
  amount: z.string(),
  discountTaken: z.string(),
  paidFromAccountId: z.string().min(1, 'Required'),
  referenceNumber: z.string().optional(),
})

type FormValues = z.infer<typeof formSchema>

interface BillPaymentFormModalProps {
  bill: Bill | null
  onClose: () => void
}

export function BillPaymentFormModal({ bill, onClose }: BillPaymentFormModalProps) {
  const queryClient = useQueryClient()

  const { data: accounts } = useQuery({
    queryKey: ['accounts', 'ACTIVE'],
    queryFn: () => accountsApi.list('ACTIVE'),
    enabled: Boolean(bill),
  })
  const paymentAccounts = (accounts ?? []).filter((a) => a.accountType === 'BANK' || a.accountType === 'CASH')

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    values: {
      paymentDate: new Date().toISOString().slice(0, 10),
      amount: bill?.balanceDue ?? '0',
      discountTaken: '0',
      paidFromAccountId: '',
      referenceNumber: '',
    },
  })

  const amount = Number(watch('amount')) || 0
  const discountTaken = Number(watch('discountTaken')) || 0
  const balanceDue = Number(bill?.balanceDue ?? 0)
  const overpayment = amount + discountTaken > balanceDue ? amount + discountTaken - balanceDue : 0

  function applyDiscount() {
    if (!bill) return
    const discountAmount = Number(bill.earlyPaymentDiscountAmount) || 0
    setValue('discountTaken', String(discountAmount))
    setValue('amount', String(Math.max(balanceDue - discountAmount, 0)))
  }

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      if (!bill) throw new Error('No bill selected')
      const amountValue = Number(values.amount) || 0
      const discountValue = Number(values.discountTaken) || 0
      const applied = Math.min(amountValue + discountValue, balanceDue)
      return billPaymentsApi.create({
        vendorId: bill.vendorId,
        paymentDate: values.paymentDate,
        amount: amountValue,
        paidFromAccountId: values.paidFromAccountId,
        referenceNumber: values.referenceNumber || undefined,
        applications:
          applied > 0
            ? [{ billId: bill.id, amount: Math.min(amountValue, applied), discountTaken: discountValue }]
            : [],
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bills'] })
      queryClient.invalidateQueries({ queryKey: ['bill-payments'] })
      toast.success('Bill payment recorded')
      reset()
      onClose()
    },
    onError: (error) => {
      toast.error('Could not record bill payment', error instanceof ApiError ? error.message : undefined)
    },
  })

  if (!bill) return null

  return (
    <Modal open={Boolean(bill)} onClose={onClose} title={`Pay Bill — ${bill.billNumber}`}>
      <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
        <p className="text-sm text-slate-500">
          Balance due: <strong className="tabular-nums">{bill.balanceDue}</strong> {bill.currencyCode}
        </p>

        {bill.eligibleForEarlyPaymentDiscountToday && Number(bill.earlyPaymentDiscountAmount) > 0 && (
          <div className="flex items-center justify-between rounded-md bg-emerald-50 px-3 py-2 text-xs text-emerald-800">
            <span>
              Eligible for an early-payment discount of{' '}
              <strong className="tabular-nums">{bill.earlyPaymentDiscountAmount}</strong> today.
            </span>
            <Button type="button" variant="ghost" size="sm" onClick={applyDiscount}>
              Apply Discount
            </Button>
          </div>
        )}

        <div className="grid grid-cols-2 gap-4">
          <Field label="Payment Date" htmlFor="paymentDate" required error={errors.paymentDate?.message}>
            <Input id="paymentDate" type="date" {...register('paymentDate')} />
          </Field>
          <Field label="Amount" htmlFor="amount">
            <Input id="amount" type="number" step="0.01" min="0" {...register('amount')} />
          </Field>
        </div>

        <Field
          label="Discount Taken"
          htmlFor="discountTaken"
          hint="Early-payment discount applied to this payment"
        >
          <Input id="discountTaken" type="number" step="0.01" min="0" {...register('discountTaken')} />
        </Field>

        <Field
          label="Pay From"
          htmlFor="paidFromAccountId"
          required
          error={errors.paidFromAccountId?.message}
        >
          <Select id="paidFromAccountId" {...register('paidFromAccountId')}>
            <option value="">Select an account…</option>
            {paymentAccounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.code} — {a.name}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Reference #" htmlFor="referenceNumber">
          <Input id="referenceNumber" {...register('referenceNumber')} />
        </Field>

        {overpayment > 0 && (
          <p className="rounded-md bg-amber-50 px-3 py-2 text-xs text-amber-800">
            This payment plus discount exceeds the balance due by {overpayment.toFixed(2)}.
          </p>
        )}

        <div className="mt-2 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            Record Payment
          </Button>
        </div>
      </form>
    </Modal>
  )
}
