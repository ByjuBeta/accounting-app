import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { statusTone } from '@/lib/statusTone'
import { customersApi, type Customer } from '@/api/customers'
import { CustomerFormModal } from './CustomerFormModal'

export function CustomersPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null)

  const { data: customers, isLoading } = useQuery({
    queryKey: ['customers'],
    queryFn: customersApi.list,
  })

  function openCreateModal() {
    setEditingCustomer(null)
    setModalOpen(true)
  }

  function openEditModal(customer: Customer) {
    setEditingCustomer(customer)
    setModalOpen(true)
  }

  return (
    <div>
      <PageHeader
        title="Customers"
        description="Customer master list and payment terms."
        actions={<Button onClick={openCreateModal}>+ Add Customer</Button>}
      />

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : !customers || customers.length === 0 ? (
        <EmptyState
          title="No customers yet"
          description="Add a customer before creating invoices."
          action={<Button onClick={openCreateModal}>+ Add Customer</Button>}
        />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH>Name</TH>
              <TH>Company</TH>
              <TH>Email</TH>
              <TH>Terms</TH>
              <TH>Status</TH>
            </TR>
          </THead>
          <TBody>
            {customers.map((customer) => (
              <TR key={customer.id} className="cursor-pointer" onClick={() => openEditModal(customer)}>
                <TD className="font-medium text-slate-900">{customer.name}</TD>
                <TD>{customer.companyName ?? '—'}</TD>
                <TD>{customer.email ?? '—'}</TD>
                <TD>Net {customer.paymentTermsDays}</TD>
                <TD>
                  <Badge tone={statusTone(customer.status)}>{customer.status}</Badge>
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      )}

      <CustomerFormModal open={modalOpen} onClose={() => setModalOpen(false)} customer={editingCustomer} />
    </div>
  )
}
