import { describe, expect, it } from 'vitest'
import { formatCurrency, formatDate } from './format'

describe('formatCurrency', () => {
  it('formats a positive number as USD by default', () => {
    expect(formatCurrency(1234.5)).toBe('$1,234.50')
  })

  it('formats a numeric string', () => {
    expect(formatCurrency('99.9')).toBe('$99.90')
  })

  it('formats negative amounts with a leading minus sign', () => {
    expect(formatCurrency(-245)).toBe('-$245.00')
  })

  it('respects a non-default currency code', () => {
    expect(formatCurrency(10, 'EUR')).toBe('€10.00')
  })
})

describe('formatDate', () => {
  it('formats an ISO date string', () => {
    expect(formatDate('2026-01-15')).toMatch(/Jan \d{1,2}, 2026/)
  })

  it('formats a Date object', () => {
    expect(formatDate(new Date(2026, 0, 15))).toBe('Jan 15, 2026')
  })
})
