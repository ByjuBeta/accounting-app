import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Field } from '@/components/ui/Field'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { bankTransactionsApi, type BankFeedFormat } from '@/api/banking'

interface BankImportModalProps {
  open: boolean
  accountId: string
  onClose: () => void
}

export function BankImportModal({ open, accountId, onClose }: BankImportModalProps) {
  const queryClient = useQueryClient()
  const [format, setFormat] = useState<BankFeedFormat>('CSV')
  const [file, setFile] = useState<File | null>(null)

  const mutation = useMutation({
    mutationFn: () => {
      if (!file) throw new Error('No file selected')
      return bankTransactionsApi.importFeed(accountId, format, file)
    },
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['bank-transactions', accountId] })
      toast.success(
        'Import complete',
        `${result.imported} imported, ${result.skippedDuplicates} duplicate(s) skipped`,
      )
      closeAndReset()
    },
    onError: (error) => {
      toast.error('Could not import transactions', error instanceof ApiError ? error.message : undefined)
    },
  })

  function closeAndReset() {
    setFile(null)
    setFormat('CSV')
    onClose()
  }

  return (
    <Modal open={open} onClose={closeAndReset} title="Import Bank Transactions">
      <div className="flex flex-col gap-4">
        <Field label="Format" htmlFor="format">
          <Select id="format" value={format} onChange={(e) => setFormat(e.target.value as BankFeedFormat)}>
            <option value="CSV">CSV</option>
            <option value="OFX">OFX</option>
          </Select>
        </Field>

        <Field
          label="File"
          htmlFor="file"
          hint="Duplicate transactions (matching external ID) are skipped automatically"
        >
          <input
            id="file"
            type="file"
            accept={format === 'CSV' ? '.csv' : '.ofx,.qfx'}
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            className="text-sm text-slate-600 file:mr-3 file:rounded-md file:border-0 file:bg-slate-100 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-slate-700 hover:file:bg-slate-200"
          />
        </Field>

        <div className="mt-2 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={closeAndReset}>
            Cancel
          </Button>
          <Button type="button" onClick={() => mutation.mutate()} disabled={!file || mutation.isPending}>
            Import
          </Button>
        </div>
      </div>
    </Modal>
  )
}
