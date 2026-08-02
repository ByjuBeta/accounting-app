import { useForm, useFieldArray, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Field } from '@/components/ui/Field'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Textarea } from '@/components/ui/Textarea'
import { Button } from '@/components/ui/Button'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { accountsApi } from '@/api/accounts'
import { journalEntriesApi, TRANSACTION_TYPE_LABELS, type TransactionType } from '@/api/journalEntries'
import { AccountPicker } from './AccountPicker'

const TRANSACTION_TYPES = Object.keys(TRANSACTION_TYPE_LABELS) as TransactionType[]

const lineSchema = z.object({
  accountId: z.string().min(1, 'Required'),
  debitAmount: z.string(),
  creditAmount: z.string(),
  memo: z.string().optional(),
})

const formSchema = z.object({
  entryDate: z.string().min(1, 'Required'),
  transactionType: z.string().min(1, 'Required'),
  memo: z.string().optional(),
  referenceNumber: z.string().optional(),
  lines: z.array(lineSchema).min(2, 'At least two lines are required'),
})

type FormValues = z.infer<typeof formSchema>

const emptyLine = { accountId: '', debitAmount: '0', creditAmount: '0', memo: '' }

interface JournalEntryFormModalProps {
  open: boolean
  onClose: () => void
}

export function JournalEntryFormModal({ open, onClose }: JournalEntryFormModalProps) {
  const queryClient = useQueryClient()

  const { data: accounts } = useQuery({
    queryKey: ['accounts', 'ACTIVE'],
    queryFn: () => accountsApi.list('ACTIVE'),
    enabled: open,
  })

  const {
    register,
    control,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      entryDate: new Date().toISOString().slice(0, 10),
      transactionType: 'JOURNAL_ENTRY',
      memo: '',
      referenceNumber: '',
      lines: [emptyLine, emptyLine],
    },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'lines' })
  const lines = watch('lines')
  const totalDebit = lines.reduce((sum, l) => sum + (Number(l.debitAmount) || 0), 0)
  const totalCredit = lines.reduce((sum, l) => sum + (Number(l.creditAmount) || 0), 0)
  const difference = Math.round((totalDebit - totalCredit) * 100) / 100
  const isBalanced = difference === 0 && totalDebit > 0

  const save = useMutation({
    mutationFn: async ({ values, post }: { values: FormValues; post: boolean }) => {
      const entry = await journalEntriesApi.create({
        entryDate: values.entryDate,
        transactionType: values.transactionType as TransactionType,
        memo: values.memo || undefined,
        referenceNumber: values.referenceNumber || undefined,
        lines: values.lines.map((l) => ({
          accountId: l.accountId,
          debitAmount: Number(l.debitAmount) || 0,
          creditAmount: Number(l.creditAmount) || 0,
          memo: l.memo || undefined,
        })),
      })
      return post ? journalEntriesApi.post(entry.id) : entry
    },
    onSuccess: (_entry, { post }) => {
      queryClient.invalidateQueries({ queryKey: ['journal-entries'] })
      toast.success(post ? 'Journal entry posted' : 'Draft saved')
      reset()
      onClose()
    },
    onError: (error) => {
      toast.error('Could not save journal entry', error instanceof ApiError ? error.message : undefined)
    },
  })

  function closeAndReset() {
    reset()
    onClose()
  }

  return (
    <Modal open={open} onClose={closeAndReset} title="New Journal Entry" size="xl">
      <form className="flex flex-col gap-4">
        <div className="grid grid-cols-3 gap-4">
          <Field label="Date" htmlFor="entryDate" required error={errors.entryDate?.message}>
            <Input id="entryDate" type="date" {...register('entryDate')} />
          </Field>
          <Field label="Type" htmlFor="transactionType" required>
            <Select id="transactionType" {...register('transactionType')}>
              {TRANSACTION_TYPES.map((type) => (
                <option key={type} value={type}>
                  {TRANSACTION_TYPE_LABELS[type]}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Reference #" htmlFor="referenceNumber">
            <Input id="referenceNumber" {...register('referenceNumber')} />
          </Field>
        </div>

        <Field label="Memo" htmlFor="memo">
          <Textarea id="memo" rows={2} {...register('memo')} />
        </Field>

        <div>
          <div className="mb-2 grid grid-cols-12 gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
            <div className="col-span-5">Account</div>
            <div className="col-span-2 text-right">Debit</div>
            <div className="col-span-2 text-right">Credit</div>
            <div className="col-span-2">Memo</div>
            <div className="col-span-1" />
          </div>
          <div className="flex flex-col gap-2">
            {fields.map((field, index) => (
              <div key={field.id} className="grid grid-cols-12 items-start gap-2">
                <div className="col-span-5">
                  <Controller
                    control={control}
                    name={`lines.${index}.accountId`}
                    render={({ field: f }) => <AccountPicker accounts={accounts ?? []} {...f} />}
                  />
                  {errors.lines?.[index]?.accountId && (
                    <p className="mt-1 text-xs text-red-600">{errors.lines[index]?.accountId?.message}</p>
                  )}
                </div>
                <div className="col-span-2">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    className="text-right tabular-nums"
                    {...register(`lines.${index}.debitAmount`)}
                    onChange={(e) => {
                      setValue(`lines.${index}.debitAmount`, e.target.value)
                      if (e.target.valueAsNumber > 0) setValue(`lines.${index}.creditAmount`, '0')
                    }}
                  />
                </div>
                <div className="col-span-2">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    className="text-right tabular-nums"
                    {...register(`lines.${index}.creditAmount`)}
                    onChange={(e) => {
                      setValue(`lines.${index}.creditAmount`, e.target.value)
                      if (e.target.valueAsNumber > 0) setValue(`lines.${index}.debitAmount`, '0')
                    }}
                  />
                </div>
                <div className="col-span-2">
                  <Input {...register(`lines.${index}.memo`)} />
                </div>
                <div className="col-span-1 pt-1.5">
                  <button
                    type="button"
                    onClick={() => remove(index)}
                    disabled={fields.length <= 2}
                    aria-label="Remove line"
                    className="text-slate-400 hover:text-red-600 disabled:opacity-30"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>
          {errors.lines?.root?.message && (
            <p className="mt-1 text-xs text-red-600">{errors.lines.root.message}</p>
          )}
          <Button type="button" variant="ghost" size="sm" className="mt-2" onClick={() => append(emptyLine)}>
            + Add Line
          </Button>
        </div>

        <div className="flex items-center justify-end gap-6 rounded-md bg-slate-50 px-4 py-2.5 text-sm">
          <span className="tabular-nums">
            Debit: <strong>{totalDebit.toFixed(2)}</strong>
          </span>
          <span className="tabular-nums">
            Credit: <strong>{totalCredit.toFixed(2)}</strong>
          </span>
          <span className={isBalanced ? 'font-medium text-green-600' : 'font-medium text-red-600'}>
            {isBalanced ? '✓ Balanced' : `Out of balance by ${Math.abs(difference).toFixed(2)}`}
          </span>
        </div>

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={closeAndReset}>
            Cancel
          </Button>
          <Button
            type="button"
            variant="secondary"
            disabled={!isBalanced || save.isPending}
            onClick={handleSubmit((values) => save.mutate({ values, post: false }))}
          >
            Save as Draft
          </Button>
          <Button
            type="button"
            disabled={!isBalanced || save.isPending}
            onClick={handleSubmit((values) => save.mutate({ values, post: true }))}
          >
            Save &amp; Post
          </Button>
        </div>
      </form>
    </Modal>
  )
}
