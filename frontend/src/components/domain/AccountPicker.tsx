import { forwardRef, type SelectHTMLAttributes } from 'react'
import { Select } from '@/components/ui/Select'
import type { Account, AccountCategory } from '@/api/accounts'

const CATEGORY_LABELS: Record<AccountCategory, string> = {
  ASSET: 'Assets',
  LIABILITY: 'Liabilities',
  EQUITY: 'Equity',
  REVENUE: 'Revenue',
  EXPENSE: 'Expenses',
}

const CATEGORY_ORDER: AccountCategory[] = ['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE']

interface AccountPickerProps extends SelectHTMLAttributes<HTMLSelectElement> {
  accounts: Account[]
}

export const AccountPicker = forwardRef<HTMLSelectElement, AccountPickerProps>(function AccountPicker(
  { accounts, ...props },
  ref,
) {
  return (
    <Select ref={ref} {...props}>
      <option value="">Select an account…</option>
      {CATEGORY_ORDER.map((category) => {
        const options = accounts.filter((a) => a.category === category)
        if (options.length === 0) return null
        return (
          <optgroup key={category} label={CATEGORY_LABELS[category]}>
            {options.map((account) => (
              <option key={account.id} value={account.id}>
                {account.code} — {account.name}
              </option>
            ))}
          </optgroup>
        )
      })}
    </Select>
  )
})
