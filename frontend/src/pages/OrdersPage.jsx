import { useState } from 'react'
import { Link } from 'react-router-dom'

import { fetchOrders } from '../api/orders'
import { OrderStatusBadge } from '../components/order/OrderBits'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { PackageIcon } from '../components/ui/icons'
import { Skeleton } from '../components/ui/Loader'
import Pagination from '../components/ui/Pagination'
import useAsync, { describeError } from '../hooks/useAsync'
import { formatWhen } from '../lib/dates'
import { formatRupees } from '../lib/money'

/** My orders, newest first. */
export default function OrdersPage() {
  const [page, setPage] = useState(0)
  const { data, loading, error } = useAsync(() => fetchOrders(page), [page])

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-white">My orders</h1>
      {loading ? <div className="space-y-3"><Skeleton className="h-20" /><Skeleton className="h-20" /></div>
        : error ? <EmptyState title="Could not load your orders" message={describeError(error).message} />
          : data.content.length === 0 ? (
            <EmptyState icon={PackageIcon} title="No orders yet" message="When you pay for a cart, it shows up here."
              action={<Button to="/products">Start shopping</Button>} />
          ) : (
            <ul className="space-y-3">
              {data.content.map((o) => (
                <li key={o.number}>
                  <Link to={`/orders/${o.number}`} className="block">
                    <Card className="flex flex-wrap items-center gap-x-4 gap-y-1 p-4 transition hover:shadow-md">
                      <div className="min-w-0 flex-1">
                        <p className="font-semibold text-slate-900 dark:text-white">{o.number}</p>
                        <p className="truncate text-sm text-slate-600 dark:text-slate-400">
                          {o.firstItemName}{o.itemCount > 1 && ` + ${o.itemCount - 1} more`} · {o.packageCount} {o.packageCount === 1 ? 'package' : 'packages'}
                        </p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">{formatWhen(o.createdAt)}</p>
                      </div>
                      <div className="text-right">
                        <p className="font-bold text-slate-900 dark:text-white">{formatRupees(o.grandTotal)}</p>
                        <OrderStatusBadge status={o.status} />
                      </div>
                    </Card>
                  </Link>
                </li>
              ))}
            </ul>
          )}
      {data && data.totalPages > 1 && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  )
}
