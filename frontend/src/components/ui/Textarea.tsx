import { forwardRef, type TextareaHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement>

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { className, ...props },
  ref,
) {
  return (
    <textarea
      ref={ref}
      className={cn(
        'block w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900',
        'placeholder:text-slate-400',
        'focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500',
        'disabled:bg-slate-100 disabled:text-slate-500',
        className,
      )}
      {...props}
    />
  )
})
