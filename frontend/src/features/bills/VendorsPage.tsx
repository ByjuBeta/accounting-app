import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { statusTone } from '@/lib/statusTone'
import { vendorsApi, type Vendor } from '@/api/vendors'
import { VendorFormModal } from './VendorFormModal'

export function VendorsPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingVendor, setEditingVendor] = useState<Vendor | null>(null)

  const { data: vendors, isLoading } = useQuery({
    queryKey: ['vendors'],
    queryFn: vendorsApi.list,
  })

  function openCreateModal() {
    setEditingVendor(null)
    setModalOpen(true)
  }

  function openEditModal(vendor: Vendor) {
    setEditingVendor(vendor)
    setModalOpen(true)
  }

  return (
    <div>
      <PageHeader
        title="Vendors"
        description="Vendor master list and payment terms."
        actions={<Button onClick={openCreateModal}>+ Add Vendor</Button>}
      />

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : !vendors || vendors.length === 0 ? (
        <EmptyState
          title="No vendors yet"
          description="Add a vendor before entering bills."
          action={<Button onClick={openCreateModal}>+ Add Vendor</Button>}
        />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH>Name</TH>
              <TH>Company</TH>
              <TH>Email</TH>
              <TH>Terms</TH>
              <TH>Discount</TH>
              <TH>Status</TH>
            </TR>
          </THead>
          <TBody>
            {vendors.map((vendor) => (
              <TR key={vendor.id} className="cursor-pointer" onClick={() => openEditModal(vendor)}>
                <TD className="font-medium text-slate-900">{vendor.name}</TD>
                <TD>{vendor.companyName ?? '—'}</TD>
                <TD>{vendor.email ?? '—'}</TD>
                <TD>Net {vendor.paymentTermsDays}</TD>
                <TD>
                  {Number(vendor.earlyPaymentDiscountPercent) > 0
                    ? `${vendor.earlyPaymentDiscountPercent}/${vendor.earlyPaymentDiscountDays}`
                    : '—'}
                </TD>
                <TD>
                  <Badge tone={statusTone(vendor.status)}>{vendor.status}</Badge>
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      )}

      <VendorFormModal open={modalOpen} onClose={() => setModalOpen(false)} vendor={editingVendor} />
    </div>
  )
}
