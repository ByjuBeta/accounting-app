import { createPortal } from 'react-dom'
import { useToastStore } from '@/store/useToastStore'
import { cn } from '@/lib/cn'

const VARIANT_CLASSES = {
  success: 'border-green-200 bg-green-50 text-green-900',
  error: 'border-red-200 bg-red-50 text-red-900',
  info: 'border-slate-200 bg-white text-slate-900',
}

export function Toaster() {
  const { toasts, dismiss } = useToastStore()

  return createPortal(
    <div className="fixed bottom-4 right-4 z-[100] flex w-80 flex-col gap-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          role="status"
          className={cn('rounded-lg border p-3 shadow-md', VARIANT_CLASSES[t.variant])}
        >
          <div className="flex items-start justify-between gap-2">
            <div>
              <div className="text-sm font-medium">{t.title}</div>
              {t.description && <div className="mt-0.5 text-xs opacity-80">{t.description}</div>}
            </div>
            <button
              type="button"
              onClick={() => dismiss(t.id)}
              aria-label="Dismiss"
              className="text-current opacity-50 hover:opacity-100"
            >
              ✕
            </button>
          </div>
        </div>
      ))}
    </div>,
    document.body,
  )
}
