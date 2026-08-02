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
import { paymentsApi } from '@/api/payments'
import type { Invoice } from '@/api/invoices'

const formSchema = z.object({
  paymentDate: z.string().min(1, 'Required'),
  amount: z.string(),
  depositToAccountId: z.string().min(1, 'Required'),
  referenceNumber: z.string().optional(),
})

type FormValues = z.infer<typeof formSchema>

interface PaymentFormModalProps {
  invoice: Invoice | null
  onClose: () => void
}

export function PaymentFormModal({ invoice, onClose }: PaymentFormModalProps) {
  const queryClient = useQueryClient()

  const { data: accounts } = useQuery({
    queryKey: ['accounts', 'ACTIVE'],
    queryFn: () => accountsApi.list('ACTIVE'),
    enabled: Boolean(invoice),
  })
  const depositAccounts = (accounts ?? []).filter((a) => a.accountType === 'BANK' || a.accountType === 'CASH')

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    values: {
      paymentDate: new Date().toISOString().slice(0, 10),
      amount: invoice?.balanceDue ?? '0',
      depositToAccountId: '',
      referenceNumber: '',
    },
  })

  const amount = Number(watch('amount')) || 0
  const balanceDue = Number(invoice?.balanceDue ?? 0)
  const overpayment = amount > balanceDue ? amount - balanceDue : 0

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      if (!invoice) throw new Error('No invoice selected')
      const amountValue = Number(values.amount) || 0
      const applied = Math.min(amountValue, balanceDue)
      return paymentsApi.create({
        customerId: invoice.customerId,
        paymentDate: values.paymentDate,
        amount: amountValue,
        depositToAccountId: values.depositToAccountId,
        referenceNumber: values.referenceNumber || undefined,
        applications: applied > 0 ? [{ invoiceId: invoice.id, amount: applied }] : [],
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      queryClient.invalidateQueries({ queryKey: ['payments'] })
      toast.success('Payment recorded')
      reset()
      onClose()
    },
    onError: (error) => {
      toast.error('Could not record payment', error instanceof ApiError ? error.message : undefined)
    },
  })

  if (!invoice) return null

  return (
    <Modal open={Boolean(invoice)} onClose={onClose} title={`Record Payment — ${invoice.invoiceNumber}`}>
      <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
        <p className="text-sm text-slate-500">
          Balance due: <strong className="tabular-nums">{invoice.balanceDue}</strong> {invoice.currencyCode}
        </p>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Payment Date" htmlFor="paymentDate" required error={errors.paymentDate?.message}>
            <Input id="paymentDate" type="date" {...register('paymentDate')} />
          </Field>
          <Field label="Amount" htmlFor="amount">
            <Input id="amount" type="number" step="0.01" min="0" {...register('amount')} />
          </Field>
        </div>

        <Field
          label="Deposit To"
          htmlFor="depositToAccountId"
          required
          error={errors.depositToAccountId?.message}
        >
          <Select id="depositToAccountId" {...register('depositToAccountId')}>
            <option value="">Select an account…</option>
            {depositAccounts.map((a) => (
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
            This payment exceeds the balance due by {overpayment.toFixed(2)} — the extra amount will be
            recorded as a customer credit.
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
