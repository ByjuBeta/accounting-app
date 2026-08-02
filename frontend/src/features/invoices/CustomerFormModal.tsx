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
import { customersApi, type Customer } from '@/api/customers'

const formSchema = z.object({
  name: z.string().min(1, 'Required').max(255),
  companyName: z.string().optional(),
  email: z.string().email('Enter a valid email').optional().or(z.literal('')),
  phone: z.string().optional(),
  line1: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
  postalCode: z.string().optional(),
  country: z.string().optional(),
  paymentTermsDays: z.string(),
  taxExempt: z.boolean(),
  notes: z.string().optional(),
  status: z.string().optional(),
})

type FormValues = z.infer<typeof formSchema>

interface CustomerFormModalProps {
  open: boolean
  onClose: () => void
  customer?: Customer | null
}

export function CustomerFormModal({ open, onClose, customer }: CustomerFormModalProps) {
  const queryClient = useQueryClient()
  const isEditing = Boolean(customer)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    values: customer
      ? {
          name: customer.name,
          companyName: customer.companyName ?? '',
          email: customer.email ?? '',
          phone: customer.phone ?? '',
          line1: customer.billingAddress?.line1 ?? '',
          city: customer.billingAddress?.city ?? '',
          state: customer.billingAddress?.state ?? '',
          postalCode: customer.billingAddress?.postalCode ?? '',
          country: customer.billingAddress?.country ?? '',
          paymentTermsDays: String(customer.paymentTermsDays),
          taxExempt: customer.taxExempt,
          notes: customer.notes ?? '',
          status: customer.status,
        }
      : {
          name: '',
          companyName: '',
          email: '',
          phone: '',
          line1: '',
          city: '',
          state: '',
          postalCode: '',
          country: '',
          paymentTermsDays: '30',
          taxExempt: false,
          notes: '',
        },
  })

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const billingAddress = {
        line1: values.line1 || undefined,
        city: values.city || undefined,
        state: values.state || undefined,
        postalCode: values.postalCode || undefined,
        country: values.country || undefined,
      }
      const payload = {
        name: values.name,
        companyName: values.companyName || undefined,
        email: values.email || undefined,
        phone: values.phone || undefined,
        billingAddress,
        paymentTermsDays: Number(values.paymentTermsDays) || 30,
        taxExempt: values.taxExempt,
        notes: values.notes || undefined,
      }
      if (isEditing && customer) {
        return customersApi.update(customer.id, {
          ...payload,
          status: values.status as 'ACTIVE' | 'INACTIVE' | undefined,
        })
      }
      return customersApi.create(payload)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      toast.success(isEditing ? 'Customer updated' : 'Customer created')
      reset()
      onClose()
    },
    onError: (error) => {
      toast.error('Could not save customer', error instanceof ApiError ? error.message : undefined)
    },
  })

  return (
    <Modal open={open} onClose={onClose} title={isEditing ? 'Edit Customer' : 'New Customer'} size="lg">
      <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-4">
          <Field label="Name" htmlFor="name" required error={errors.name?.message}>
            <Input id="name" {...register('name')} />
          </Field>
          <Field label="Company" htmlFor="companyName">
            <Input id="companyName" {...register('companyName')} />
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Email" htmlFor="email" error={errors.email?.message}>
            <Input id="email" type="email" {...register('email')} />
          </Field>
          <Field label="Phone" htmlFor="phone">
            <Input id="phone" {...register('phone')} />
          </Field>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Address" htmlFor="line1">
            <Input id="line1" {...register('line1')} />
          </Field>
          <Field label="City" htmlFor="city">
            <Input id="city" {...register('city')} />
          </Field>
        </div>
        <div className="grid grid-cols-3 gap-4">
          <Field label="State" htmlFor="state">
            <Input id="state" {...register('state')} />
          </Field>
          <Field label="Postal Code" htmlFor="postalCode">
            <Input id="postalCode" {...register('postalCode')} />
          </Field>
          <Field label="Country" htmlFor="country">
            <Input id="country" {...register('country')} />
          </Field>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Payment Terms (days)" htmlFor="paymentTermsDays">
            <Input id="paymentTermsDays" type="number" min="0" {...register('paymentTermsDays')} />
          </Field>
          {isEditing && (
            <Field label="Status" htmlFor="status">
              <Select id="status" {...register('status')}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </Select>
            </Field>
          )}
        </div>

        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" className="rounded border-slate-300" {...register('taxExempt')} />
          Tax exempt
        </label>

        <Field label="Notes" htmlFor="notes">
          <Textarea id="notes" rows={2} {...register('notes')} />
        </Field>

        <div className="mt-2 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isEditing ? 'Save Changes' : 'Create Customer'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
