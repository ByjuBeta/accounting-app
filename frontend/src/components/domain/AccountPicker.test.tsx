import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AccountPicker } from './AccountPicker'
import type { Account } from '@/api/accounts'

function makeAccount(overrides: Partial<Account>): Account {
  return {
    id: 'id',
    version: 0,
    code: '1000',
    name: 'Account',
    description: null,
    accountType: 'BANK',
    category: 'ASSET',
    normalBalance: 'DEBIT',
    parentId: null,
    currencyCode: 'USD',
    status: 'ACTIVE',
    tags: [],
    ...overrides,
  }
}

const accounts: Account[] = [
  makeAccount({ id: 'bank-1', code: '1010', name: 'Checking', category: 'ASSET' }),
  makeAccount({ id: 'rev-1', code: '4000', name: 'Sales', category: 'REVENUE' }),
  makeAccount({ id: 'exp-1', code: '6000', name: 'Rent', category: 'EXPENSE' }),
]

describe('AccountPicker', () => {
  it('always renders a placeholder option first', () => {
    render(<AccountPicker accounts={accounts} onChange={vi.fn()} />)
    expect(screen.getByRole('option', { name: 'Select an account…' })).toBeInTheDocument()
  })

  it('groups accounts under their category, in category order', () => {
    render(<AccountPicker accounts={accounts} onChange={vi.fn()} />)
    const groups = screen.getAllByRole('group').map((g) => g.getAttribute('label'))
    expect(groups).toEqual(['Assets', 'Revenue', 'Expenses'])
  })

  it('omits categories with no matching accounts', () => {
    render(<AccountPicker accounts={[accounts[0]!]} onChange={vi.fn()} />)
    expect(screen.queryByRole('group', { name: 'Revenue' })).not.toBeInTheDocument()
  })

  it('fires onChange with the selected account id', async () => {
    const onChange = vi.fn()
    render(<AccountPicker accounts={accounts} onChange={onChange} />)
    await userEvent.selectOptions(screen.getByRole('combobox'), 'rev-1')
    expect(onChange).toHaveBeenCalled()
  })
})
