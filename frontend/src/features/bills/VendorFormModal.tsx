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
import { vendorsApi, type Vendor } from '@/api/vendors'

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
  earlyPaymentDiscountPercent: z.string(),
  earlyPaymentDiscountDays: z.string(),
  taxId: z.string().optional(),
  notes: z.string().optional(),
  status: z.string().optional(),
})

type FormValues = z.infer<typeof formSchema>

interface VendorFormModalProps {
  open: boolean
  onClose: () => void
  vendor?: Vendor | null
}

export function VendorFormModal({ open, onClose, vendor }: VendorFormModalProps) {
  const queryClient = useQueryClient()
  const isEditing = Boolean(vendor)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    values: vendor
      ? {
          name: vendor.name,
          companyName: vendor.companyName ?? '',
          email: vendor.email ?? '',
          phone: vendor.phone ?? '',
          line1: vendor.address?.line1 ?? '',
          city: vendor.address?.city ?? '',
          state: vendor.address?.state ?? '',
          postalCode: vendor.address?.postalCode ?? '',
          country: vendor.address?.country ?? '',
          paymentTermsDays: String(vendor.paymentTermsDays),
          earlyPaymentDiscountPercent: vendor.earlyPaymentDiscountPercent,
          earlyPaymentDiscountDays: String(vendor.earlyPaymentDiscountDays),
          taxId: vendor.taxId ?? '',
          notes: vendor.notes ?? '',
          status: vendor.status,
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
          earlyPaymentDiscountPercent: '0',
          earlyPaymentDiscountDays: '0',
          taxId: '',
          notes: '',
        },
  })

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const address = {
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
        address,
        paymentTermsDays: Number(values.paymentTermsDays) || 30,
        earlyPaymentDiscountPercent: Number(values.earlyPaymentDiscountPercent) || 0,
        earlyPaymentDiscountDays: Number(values.earlyPaymentDiscountDays) || 0,
        taxId: values.taxId || undefined,
        notes: values.notes || undefined,
      }
      if (isEditing && vendor) {
        return vendorsApi.update(vendor.id, {
          ...payload,
          status: values.status as 'ACTIVE' | 'INACTIVE' | undefined,
        })
      }
      return vendorsApi.create(payload)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vendors'] })
      toast.success(isEditing ? 'Vendor updated' : 'Vendor created')
      reset()
      onClose()
    },
    onError: (error) => {
      toast.error('Could not save vendor', error instanceof ApiError ? error.message : undefined)
    },
  })

  return (
    <Modal open={open} onClose={onClose} title={isEditing ? 'Edit Vendor' : 'New Vendor'} size="lg">
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
          <Field label="Tax ID" htmlFor="taxId">
            <Input id="taxId" {...register('taxId')} />
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

        <div className="grid grid-cols-3 gap-4 rounded-md border border-slate-200 bg-slate-50 p-3">
          <Field label="Payment Terms (days)" htmlFor="paymentTermsDays">
            <Input id="paymentTermsDays" type="number" min="0" {...register('paymentTermsDays')} />
          </Field>
          <Field
            label="Early Payment Discount %"
            htmlFor="earlyPaymentDiscountPercent"
            hint="e.g. 2 for 2/10 net 30"
          >
            <Input
              id="earlyPaymentDiscountPercent"
              type="number"
              min="0"
              max="100"
              step="0.01"
              {...register('earlyPaymentDiscountPercent')}
            />
          </Field>
          <Field
            label="Discount Window (days)"
            htmlFor="earlyPaymentDiscountDays"
            hint="e.g. 10 for 2/10 net 30"
          >
            <Input
              id="earlyPaymentDiscountDays"
              type="number"
              min="0"
              {...register('earlyPaymentDiscountDays')}
            />
          </Field>
        </div>

        <Field label="Notes" htmlFor="notes">
          <Textarea id="notes" rows={2} {...register('notes')} />
        </Field>

        <div className="mt-2 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isEditing ? 'Save Changes' : 'Create Vendor'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
