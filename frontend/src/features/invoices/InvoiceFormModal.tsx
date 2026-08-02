import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Field } from '@/components/ui/Field'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Textarea } from '@/components/ui/Textarea'
import { Button } from '@/components/ui/Button'
import { AccountPicker } from '@/components/domain/AccountPicker'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { accountsApi } from '@/api/accounts'
import { customersApi } from '@/api/customers'
import { invoicesApi } from '@/api/invoices'

const lineSchema = z.object({
  description: z.string().min(1, 'Required'),
  quantity: z.string(),
  unitPrice: z.string(),
  taxRate: z.string(),
  incomeAccountId: z.string().min(1, 'Required'),
})

const formSchema = z.object({
  customerId: z.string().min(1, 'Required'),
  invoiceDate: z.string().min(1, 'Required'),
  dueDate: z.string().optional(),
  memo: z.string().optional(),
  lines: z.array(lineSchema).min(1, 'At least one line is required'),
})

type FormValues = z.infer<typeof formSchema>

const emptyLine = { description: '', quantity: '1', unitPrice: '0', taxRate: '0', incomeAccountId: '' }

interface InvoiceFormModalProps {
  open: boolean
  onClose: () => void
}

export function InvoiceFormModal({ open, onClose }: InvoiceFormModalProps) {
  const queryClient = useQueryClient()

  const { data: customers } = useQuery({
    queryKey: ['customers'],
    queryFn: customersApi.list,
    enabled: open,
  })
  const { data: accounts } = useQuery({
    queryKey: ['accounts', 'ACTIVE'],
    queryFn: () => accountsApi.list('ACTIVE'),
    enabled: open,
  })
  const incomeAccounts = (accounts ?? []).filter((a) => a.accountType === 'INCOME')

  const {
    register,
    control,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      customerId: '',
      invoiceDate: new Date().toISOString().slice(0, 10),
      dueDate: '',
      memo: '',
      lines: [emptyLine],
    },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'lines' })
  const lines = watch('lines')
  const subtotal = lines.reduce((sum, l) => sum + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0), 0)
  const taxTotal = lines.reduce(
    (sum, l) =>
      sum + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0) * ((Number(l.taxRate) || 0) / 100),
    0,
  )
  const total = subtotal + taxTotal

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      invoicesApi.create({
        customerId: values.customerId,
        invoiceDate: values.invoiceDate,
        dueDate: values.dueDate || undefined,
        memo: values.memo || undefined,
        lines: values.lines.map((l) => ({
          description: l.description,
          quantity: Number(l.quantity) || 0,
          unitPrice: Number(l.unitPrice) || 0,
          taxRate: Number(l.taxRate) || 0,
          incomeAccountId: l.incomeAccountId,
        })),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      toast.success('Invoice created as draft')
      reset()
      onClose()
    },
    onError: (error) => {
      toast.error('Could not create invoice', error instanceof ApiError ? error.message : undefined)
    },
  })

  function closeAndReset() {
    reset()
    onClose()
  }

  return (
    <Modal open={open} onClose={closeAndReset} title="New Invoice" size="xl">
      <form className="flex flex-col gap-4">
        <div className="grid grid-cols-3 gap-4">
          <Field label="Customer" htmlFor="customerId" required error={errors.customerId?.message}>
            <Select id="customerId" {...register('customerId')}>
              <option value="">Select a customer…</option>
              {(customers ?? []).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Invoice Date" htmlFor="invoiceDate" required error={errors.invoiceDate?.message}>
            <Input id="invoiceDate" type="date" {...register('invoiceDate')} />
          </Field>
          <Field label="Due Date" htmlFor="dueDate" hint="Defaults to customer terms">
            <Input id="dueDate" type="date" {...register('dueDate')} />
          </Field>
        </div>

        <Field label="Memo" htmlFor="memo">
          <Textarea id="memo" rows={2} {...register('memo')} />
        </Field>

        <div>
          <div className="mb-2 grid grid-cols-12 gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
            <div className="col-span-4">Description</div>
            <div className="col-span-2 text-right">Qty</div>
            <div className="col-span-2 text-right">Rate</div>
            <div className="col-span-1 text-right">Tax %</div>
            <div className="col-span-2">Income Account</div>
            <div className="col-span-1" />
          </div>
          <div className="flex flex-col gap-2">
            {fields.map((field, index) => (
              <div key={field.id} className="grid grid-cols-12 items-start gap-2">
                <div className="col-span-4">
                  <Input {...register(`lines.${index}.description`)} />
                  {errors.lines?.[index]?.description && (
                    <p className="mt-1 text-xs text-red-600">{errors.lines[index]?.description?.message}</p>
                  )}
                </div>
                <div className="col-span-2">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    className="text-right tabular-nums"
                    {...register(`lines.${index}.quantity`)}
                  />
                </div>
                <div className="col-span-2">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    className="text-right tabular-nums"
                    {...register(`lines.${index}.unitPrice`)}
                  />
                </div>
                <div className="col-span-1">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    max="100"
                    className="text-right tabular-nums"
                    {...register(`lines.${index}.taxRate`)}
                  />
                </div>
                <div className="col-span-2">
                  <AccountPicker accounts={incomeAccounts} {...register(`lines.${index}.incomeAccountId`)} />
                </div>
                <div className="col-span-1 pt-1.5">
                  <button
                    type="button"
                    onClick={() => remove(index)}
                    disabled={fields.length <= 1}
                    aria-label="Remove line"
                    className="text-slate-400 hover:text-red-600 disabled:opacity-30"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>
          <Button type="button" variant="ghost" size="sm" className="mt-2" onClick={() => append(emptyLine)}>
            + Add Line
          </Button>
        </div>

        <div className="flex justify-end gap-6 rounded-md bg-slate-50 px-4 py-2.5 text-sm">
          <span>
            Subtotal: <strong className="tabular-nums">{subtotal.toFixed(2)}</strong>
          </span>
          <span>
            Tax: <strong className="tabular-nums">{taxTotal.toFixed(2)}</strong>
          </span>
          <span>
            Total: <strong className="tabular-nums">{total.toFixed(2)}</strong>
          </span>
        </div>

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={closeAndReset}>
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handleSubmit((values) => mutation.mutate(values))}
            disabled={mutation.isPending}
          >
            Create Draft
          </Button>
        </div>
      </form>
    </Modal>
  )
}
