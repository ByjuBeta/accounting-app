import { forwardRef, type SelectHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

type SelectProps = SelectHTMLAttributes<HTMLSelectElement>

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { className, ...props },
  ref,
) {
  return (
    <select
      ref={ref}
      className={cn(
        'block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900',
        'focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500',
        'disabled:bg-slate-100 disabled:text-slate-500',
        className,
      )}
      {...props}
    />
  )
})
