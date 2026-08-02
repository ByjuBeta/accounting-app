import { describe, expect, it } from 'vitest'
import { statusTone } from './statusTone'

describe('statusTone', () => {
  it('maps known statuses to their tone', () => {
    expect(statusTone('PAID')).toBe('green')
    expect(statusTone('OVERDUE')).toBe('red')
    expect(statusTone('DRAFT')).toBe('slate')
    expect(statusTone('PARTIALLY_PAID')).toBe('amber')
    expect(statusTone('UNMATCHED')).toBe('amber')
    expect(statusTone('MATCHED')).toBe('green')
  })

  it('falls back to slate for an unrecognized status', () => {
    expect(statusTone('SOMETHING_NEW')).toBe('slate')
  })
})
