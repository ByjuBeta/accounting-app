import { createBrowserRouter } from 'react-router-dom'
import { AppShell } from '@/components/layout/AppShell'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { ChartOfAccountsPage } from '@/features/accounts/ChartOfAccountsPage'
import { JournalEntriesPage } from '@/features/journal-entries/JournalEntriesPage'
import { InvoicesPage } from '@/features/invoices/InvoicesPage'
import { CustomersPage } from '@/features/invoices/CustomersPage'
import { BillsPage } from '@/features/bills/BillsPage'
import { VendorsPage } from '@/features/bills/VendorsPage'
import { BankingPage } from '@/features/banking/BankingPage'
import { ReportsPage } from '@/features/reports/ReportsPage'
import { SettingsPage } from '@/features/settings/SettingsPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { RequireAuth } from '@/features/auth/RequireAuth'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'accounts', element: <ChartOfAccountsPage /> },
      { path: 'journal-entries', element: <JournalEntriesPage /> },
      { path: 'invoices', element: <InvoicesPage /> },
      { path: 'customers', element: <CustomersPage /> },
      { path: 'bills', element: <BillsPage /> },
      { path: 'vendors', element: <VendorsPage /> },
      { path: 'banking', element: <BankingPage /> },
      { path: 'reports', element: <ReportsPage /> },
      { path: 'settings', element: <SettingsPage /> },
    ],
  },
])
