export function formatCurrency(amount: number | string, currencyCode = 'USD'): string {
  const value = typeof amount === 'string' ? Number(amount) : amount
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: currencyCode }).format(value)
}

export function formatDate(date: string | Date): string {
  const value = typeof date === 'string' ? new Date(date) : date
  return new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'short', day: 'numeric' }).format(value)
}
