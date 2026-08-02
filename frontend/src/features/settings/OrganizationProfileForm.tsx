import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Field } from '@/components/ui/Field'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { organizationsApi } from '@/api/organizations'

const formSchema = z.object({
  name: z.string().min(1, 'Required'),
  legalName: z.string().optional(),
  timeZone: z.string().min(1, 'Required'),
})

type FormValues = z.infer<typeof formSchema>

interface OrganizationProfileFormProps {
  organizationId: string
  canEdit: boolean
}

export function OrganizationProfileForm({ organizationId, canEdit }: OrganizationProfileFormProps) {
  const queryClient = useQueryClient()

  const { data: organization, isLoading } = useQuery({
    queryKey: ['organizations', organizationId],
    queryFn: () => organizationsApi.get(organizationId),
  })

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    values: organization
      ? { name: organization.name, legalName: organization.legalName ?? '', timeZone: organization.timeZone }
      : undefined,
  })

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      organizationsApi.update(organizationId, {
        name: values.name,
        legalName: values.legalName || undefined,
        timeZone: values.timeZone,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      toast.success('Organization settings saved')
    },
    onError: (error) => {
      toast.error('Could not save settings', error instanceof ApiError ? error.message : undefined)
    },
  })

  if (isLoading || !organization) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-4">
        <Field label="Name" htmlFor="name" required error={errors.name?.message}>
          <Input id="name" disabled={!canEdit} {...register('name')} />
        </Field>
        <Field label="Legal Name" htmlFor="legalName">
          <Input id="legalName" disabled={!canEdit} {...register('legalName')} />
        </Field>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <Field label="Time Zone" htmlFor="timeZone" required error={errors.timeZone?.message}>
          <Input id="timeZone" disabled={!canEdit} {...register('timeZone')} />
        </Field>
        <Field label="Base Currency" htmlFor="baseCurrencyCode" hint="Set at creation — not editable">
          <Input id="baseCurrencyCode" value={organization.baseCurrencyCode} disabled />
        </Field>
      </div>
      {canEdit && (
        <div className="flex justify-end">
          <Button type="submit" disabled={isSubmitting || mutation.isPending}>
            Save Changes
          </Button>
        </div>
      )}
    </form>
  )
}
