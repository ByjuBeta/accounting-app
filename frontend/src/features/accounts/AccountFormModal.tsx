import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Field } from '@/components/ui/Field'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Textarea } from '@/components/ui/Textarea'
import { Button } from '@/components/ui/Button'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { accountsApi, ACCOUNT_TYPE_LABELS, type Account, type AccountType } from '@/api/accounts'
import { descendantIds } from './accountTree'

const ACCOUNT_TYPES = Object.keys(ACCOUNT_TYPE_LABELS) as AccountType[]

const formSchema = z.object({
  code: z.string().min(1, 'Required').max(32),
  name: z.string().min(1, 'Required').max(255),
  description: z.string().optional(),
  accountType: z.string().min(1, 'Required'),
  parentId: z.string().optional(),
  currencyCode: z
    .string()
    .optional()
    .refine((v) => !v || v.length === 3, 'Use a 3-letter code, e.g. USD'),
  tags: z.string().optional(),
  openingBalance: z.string().optional(),
  openingBalanceDate: z.string().optional(),
})

type FormValues = z.infer<typeof formSchema>

interface AccountFormModalProps {
  open: boolean
  onClose: () => void
  account?: Account | null
  allAccounts: Account[]
}

export function AccountFormModal({ open, onClose, account, allAccounts }: AccountFormModalProps) {
  const queryClient = useQueryClient()
  const isEditing = Boolean(account)

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    values: account
      ? {
          code: account.code,
          name: account.name,
          description: account.description ?? '',
          accountType: account.accountType,
          parentId: account.parentId ?? '',
          currencyCode: account.currencyCode,
          tags: account.tags.join(', '),
        }
      : {
          code: '',
          name: '',
          description: '',
          accountType: '',
          parentId: '',
          currencyCode: '',
          tags: '',
          openingBalance: undefined,
          openingBalanceDate: new Date().toISOString().slice(0, 10),
        },
  })

  const excludedParentIds = account
    ? new Set([account.id, ...descendantIds(allAccounts, account.id)])
    : new Set<string>()
  const parentOptions = allAccounts.filter((a) => !excludedParentIds.has(a.id))
  const openingBalance = watch('openingBalance')

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const tags = values.tags
        ? values.tags
            .split(',')
            .map((t) => t.trim())
            .filter(Boolean)
        : []
      if (isEditing && account) {
        return accountsApi.update(account.id, {
          code: values.code,
          name: values.name,
          description: values.description || undefined,
          parentId: values.parentId || null,
          tags,
        })
      }
      return accountsApi.create({
        code: values.code,
        name: values.name,
        description: values.description || undefined,
        accountType: values.accountType as AccountType,
        parentId: values.parentId || null,
        currencyCode: values.currencyCode || undefined,
        tags,
        openingBalance: values.openingBalance ? Number(values.openingBalance) : undefined,
        openingBalanceDate: values.openingBalance ? values.openingBalanceDate : undefined,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      toast.success(isEditing ? 'Account updated' : 'Account created')
      reset()
      onClose()
    },
    onError: (error) => {
      toast.error('Could not save account', error instanceof ApiError ? error.message : undefined)
    },
  })

  return (
    <Modal open={open} onClose={onClose} title={isEditing ? 'Edit Account' : 'New Account'} size="lg">
      <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-4">
          <Field label="Code" htmlFor="code" required error={errors.code?.message}>
            <Input id="code" {...register('code')} />
          </Field>
          <Field label="Name" htmlFor="name" required error={errors.name?.message}>
            <Input id="name" {...register('name')} />
          </Field>
        </div>

        <Field label="Description" htmlFor="description">
          <Textarea id="description" rows={2} {...register('description')} />
        </Field>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Account Type" htmlFor="accountType" required error={errors.accountType?.message}>
            <Select id="accountType" disabled={isEditing} {...register('accountType')}>
              <option value="">Select a type…</option>
              {ACCOUNT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {ACCOUNT_TYPE_LABELS[type]}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Parent Account" htmlFor="parentId">
            <Select id="parentId" {...register('parentId')}>
              <option value="">None (top-level)</option>
              {parentOptions.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.code} — {a.name}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Field
            label="Currency"
            htmlFor="currencyCode"
            error={errors.currencyCode?.message}
            hint="Defaults to organization currency"
          >
            <Input id="currencyCode" maxLength={3} placeholder="USD" {...register('currencyCode')} />
          </Field>
          <Field label="Tags" htmlFor="tags" hint="Comma-separated">
            <Input id="tags" {...register('tags')} />
          </Field>
        </div>

        {!isEditing && (
          <div className="grid grid-cols-2 gap-4 rounded-md border border-slate-200 bg-slate-50 p-3">
            <Field label="Opening Balance" htmlFor="openingBalance">
              <Input id="openingBalance" type="number" step="0.01" min="0" {...register('openingBalance')} />
            </Field>
            <Field label="As Of" htmlFor="openingBalanceDate">
              <Input
                id="openingBalanceDate"
                type="date"
                disabled={!openingBalance}
                {...register('openingBalanceDate')}
              />
            </Field>
          </div>
        )}

        <div className="mt-2 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isEditing ? 'Save Changes' : 'Create Account'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
