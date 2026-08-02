import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/cn'

interface NavItem {
  to: string
  label: string
  end?: boolean
}

const NAV_SECTIONS: { label: string; items: NavItem[] }[] = [
  {
    label: 'Overview',
    items: [{ to: '/', label: 'Dashboard', end: true }],
  },
  {
    label: 'Accounting',
    items: [
      { to: '/accounts', label: 'Chart of Accounts' },
      { to: '/journal-entries', label: 'Journal Entries' },
    ],
  },
  {
    label: 'Sales',
    items: [
      { to: '/invoices', label: 'Invoices' },
      { to: '/customers', label: 'Customers' },
    ],
  },
  {
    label: 'Purchases',
    items: [
      { to: '/bills', label: 'Bills' },
      { to: '/vendors', label: 'Vendors' },
    ],
  },
  {
    label: 'Banking',
    items: [{ to: '/banking', label: 'Bank Accounts' }],
  },
  {
    label: 'Insights',
    items: [{ to: '/reports', label: 'Reports' }],
  },
  {
    label: 'Admin',
    items: [{ to: '/settings', label: 'Settings' }],
  },
]

export function Sidebar() {
  return (
    <nav className="flex h-full w-60 shrink-0 flex-col gap-6 overflow-y-auto border-r border-slate-200 bg-white px-3 py-6">
      <div className="px-2 text-lg font-semibold text-slate-900">Accounting App</div>
      {NAV_SECTIONS.map((section) => (
        <div key={section.label}>
          <div className="px-2 pb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-400">
            {section.label}
          </div>
          <div className="flex flex-col gap-0.5">
            {section.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    'rounded-md px-2 py-1.5 text-sm font-medium transition-colors',
                    isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100',
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        </div>
      ))}
    </nav>
  )
}
