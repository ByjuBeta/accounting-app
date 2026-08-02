import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { Field } from '@/components/ui/Field'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/useAuthStore'

const formSchema = z.object({
  firstName: z.string().min(1, 'Required'),
  lastName: z.string().min(1, 'Required'),
  email: z.string().min(1, 'Required').email('Enter a valid email'),
  password: z.string().min(8, 'Must be at least 8 characters'),
})

type FormValues = z.infer<typeof formSchema>

export function RegisterPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(formSchema) })

  const mutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: (data) => {
      setAuth(data)
      navigate('/', { replace: true })
    },
    onError: (error) => {
      toast.error('Could not create account', error instanceof ApiError ? error.message : undefined)
    },
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="mb-1 text-lg font-semibold text-slate-900">Create your account</h1>
        <p className="mb-6 text-sm text-slate-500">Start tracking your books in minutes.</p>
        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-4">
            <Field label="First name" htmlFor="firstName" required error={errors.firstName?.message}>
              <Input id="firstName" autoComplete="given-name" {...register('firstName')} />
            </Field>
            <Field label="Last name" htmlFor="lastName" required error={errors.lastName?.message}>
              <Input id="lastName" autoComplete="family-name" {...register('lastName')} />
            </Field>
          </div>
          <Field label="Email" htmlFor="email" required error={errors.email?.message}>
            <Input id="email" type="email" autoComplete="email" {...register('email')} />
          </Field>
          <Field label="Password" htmlFor="password" required error={errors.password?.message}>
            <Input id="password" type="password" autoComplete="new-password" {...register('password')} />
          </Field>
          <Button type="submit" disabled={isSubmitting || mutation.isPending} className="mt-2">
            Create account
          </Button>
        </form>
        <p className="mt-4 text-center text-sm text-slate-500">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
