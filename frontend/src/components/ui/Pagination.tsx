import { Button } from './Button'

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <div className="mt-3 flex items-center justify-between">
      <span className="text-sm text-slate-500">
        Page {page + 1} of {totalPages}
      </span>
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          Previous
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  )
}
