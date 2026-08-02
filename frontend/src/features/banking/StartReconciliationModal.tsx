import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Field } from '@/components/ui/Field'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { reconciliationsApi } from '@/api/banking'

const formSchema = z.object({
  statementDate: z.string().min(1, 'Required'),
  statementEndingBalance: z.string().min(1, 'Required'),
})

type FormValues = z.infer<typeof formSchema>

interface StartReconciliationModalProps {
  open: boolean
  accountId: string
  onClose: () => void
}

export function StartReconciliationModal({ open, accountId, onClose }: StartReconciliationModalProps) {
  const queryClient = useQueryClient()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      statementDate: new Date().toISOString().slice(0, 10),
      statementEndingBalance: '0',
    },
  })

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      reconciliationsApi.start(accountId, {
        statementDate: values.statementDate,
        statementEndingBalance: Number(values.statementEndingBalance) || 0,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reconciliations', accountId] })
      toast.success('Reconciliation started')
      reset()
      onClose()
    },
    onError: (error) => {
      toast.error('Could not start reconciliation', error instanceof ApiError ? error.message : undefined)
    },
  })

  function closeAndReset() {
    reset()
    onClose()
  }

  return (
    <Modal open={open} onClose={closeAndReset} title="Start Reconciliation">
      <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
        <Field label="Statement Date" htmlFor="statementDate" required error={errors.statementDate?.message}>
          <Input id="statementDate" type="date" {...register('statementDate')} />
        </Field>
        <Field
          label="Statement Ending Balance"
          htmlFor="statementEndingBalance"
          required
          error={errors.statementEndingBalance?.message}
        >
          <Input
            id="statementEndingBalance"
            type="number"
            step="0.01"
            {...register('statementEndingBalance')}
          />
        </Field>

        <div className="mt-2 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={closeAndReset}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            Start
          </Button>
        </div>
      </form>
    </Modal>
  )
}
