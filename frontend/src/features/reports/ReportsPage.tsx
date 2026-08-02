import { useState } from 'react'
import { PageHeader } from '@/components/layout/PageHeader'
import { cn } from '@/lib/cn'
import { arAgingApi, type CustomerAging } from '@/api/arAging'
import { apAgingApi, type VendorAging } from '@/api/apAging'
import { BalanceSheetReport } from './BalanceSheetReport'
import { IncomeStatementReport } from './IncomeStatementReport'
import { CashFlowReport } from './CashFlowReport'
import { GeneralLedgerReport } from './GeneralLedgerReport'
import { AgingReportPanel } from './AgingReportPanel'

const TABS = [
  { key: 'balance-sheet', label: 'Balance Sheet' },
  { key: 'income-statement', label: 'Income Statement' },
  { key: 'cash-flow', label: 'Cash Flow' },
  { key: 'general-ledger', label: 'General Ledger' },
  { key: 'ar-aging', label: 'A/R Aging' },
  { key: 'ap-aging', label: 'A/P Aging' },
] as const

type TabKey = (typeof TABS)[number]['key']

export function ReportsPage() {
  const [tab, setTab] = useState<TabKey>('balance-sheet')

  return (
    <div>
      <PageHeader title="Reports" description="Financial statements and aging reports." />

      <div className="mb-5 flex flex-wrap gap-1 border-b border-slate-200">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setTab(t.key)}
            className={cn(
              '-mb-px border-b-2 px-3 py-2 text-sm font-medium',
              tab === t.key
                ? 'border-brand-600 text-brand-700'
                : 'border-transparent text-slate-500 hover:text-slate-700',
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'balance-sheet' && <BalanceSheetReport />}
      {tab === 'income-statement' && <IncomeStatementReport />}
      {tab === 'cash-flow' && <CashFlowReport />}
      {tab === 'general-ledger' && <GeneralLedgerReport />}
      {tab === 'ar-aging' && (
        <AgingReportPanel
          queryKey="ar-aging"
          fetcher={arAgingApi.get}
          toRows={(rows) =>
            (rows as CustomerAging[]).map((r) => ({ name: r.customerName, bucket: r.bucket }))
          }
          nameLabel="Customer"
          emptyTitle="No outstanding customer balances"
        />
      )}
      {tab === 'ap-aging' && (
        <AgingReportPanel
          queryKey="ap-aging"
          fetcher={apAgingApi.get}
          toRows={(rows) => (rows as VendorAging[]).map((r) => ({ name: r.vendorName, bucket: r.bucket }))}
          nameLabel="Vendor"
          emptyTitle="No outstanding vendor balances"
        />
      )}
    </div>
  )
}
