import { useState } from 'react'

import { fetchAdminOrder, fetchAdminOrders } from '../../api/admin'
import { PageTitle, SearchBox, StatusBadge, StatusTabs } from '../../components/admin/AdminBits'
import { PackageStatusBadge } from '../../components/order/OrderBits'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { formatRupees } from '../../lib/money'

/** Every order, newest first. Search by part of the order number, or by the customer's full email. Read-only. */
const TABS = [
  { value: '', label: 'All' },
  { value: 'PAID', label: 'Paid' },
  { value: 'PENDING_PAYMENT', label: 'Waiting for payment' },
  { value: 'EXPIRED', label: 'Expired' },
]

export default function AdminOrdersPage() {
  const [status, setStatus] = useState('')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [openNumber, setOpenNumber] = useState(null)
  const { data, loading, error } = useAsync(() => fetchAdminOrders(q, status, page), [q, status, page])

  return (
    <div className="space-y-4">
      <PageTitle>Orders</PageTitle>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <StatusTabs tabs={TABS} value={status} onChange={(s) => { setStatus(s); setPage(0) }} />
        <SearchBox label="orders" placeholder="TV-100123 or ravi@example.com" onSearch={(text) => { setQ(text); setPage(0) }} />
      </div>

      {loading ? <Skeleton className="h-40" /> : error ? <Card className="p-4">{describeError(error).message}</Card>
        : data.content.length === 0 ? <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">No orders found.</Card> : (
          <ul className="space-y-3">
            {data.content.map((o) => (
              <Card as="li" key={o.number} className="p-4">
                <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono font-semibold text-slate-900 dark:text-white">{o.number}</span>
                      <StatusBadge status={o.status} />
                    </div>
                    <p className="break-all text-sm text-slate-600 dark:text-slate-400">
                      {o.customerEmail} · {formatWhen(o.createdAt)} · {o.packageCount} package(s){o.couponCode ? ` · ${o.couponCode}` : ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="font-semibold text-slate-900 dark:text-white">{formatRupees(o.grandTotal)}</span>
                    <Button size="sm" variant="secondary" aria-expanded={openNumber === o.number}
                      onClick={() => setOpenNumber(openNumber === o.number ? null : o.number)}>
                      {openNumber === o.number ? 'Close' : 'Details'}
                    </Button>
                  </div>
                </div>
                {openNumber === o.number && <OrderDetails number={o.number} />}
              </Card>
            ))}
          </ul>
        )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  )
}

function OrderDetails({ number }) {
  const { data, loading, error } = useAsync(() => fetchAdminOrder(number), [number])
  if (loading) return <Skeleton className="mt-3 h-24" />
  if (error) return <p className="mt-3 text-sm">{describeError(error).message}</p>
  const o = data.order
  return (
    <div className="mt-4 space-y-3 border-t border-slate-200 pt-4 text-sm dark:border-slate-800">
      <p className="text-slate-700 dark:text-slate-300">
        Ship to {o.shipTo.name}, {o.shipTo.city} {o.shipTo.pincode} · items {formatRupees(o.itemsTotal)} − coupon {formatRupees(o.discountTotal)}
        {' '}+ delivery {formatRupees(o.shippingTotal)} = <strong>{formatRupees(o.grandTotal)}</strong>
      </p>
      {o.packages.map((p) => (
        <div key={p.id} className="rounded-lg border border-slate-200 p-3 dark:border-slate-800">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="font-medium text-slate-900 dark:text-white">{p.sellerName}</span>
            <span className="flex items-center gap-2"><PackageStatusBadge status={p.status} /> {formatRupees(p.total)}</span>
          </div>
          <ul className="mt-1 text-slate-600 dark:text-slate-400">
            {p.items.map((i) => <li key={`${p.id}-${i.productSlug}-${i.variantLabel}`}>{i.quantity} × {i.productName} ({i.variantLabel})</li>)}
          </ul>
        </div>
      ))}
    </div>
  )
}
