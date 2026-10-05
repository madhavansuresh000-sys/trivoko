import { Link, useParams } from 'react-router-dom'

import { fetchOrder } from '../api/orders'
import { OrderStatusBadge, PackageStatusBadge } from '../components/order/OrderBits'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { MapPinIcon, PackageIcon } from '../components/ui/icons'
import { Spinner } from '../components/ui/Loader'
import useAsync, { describeError } from '../hooks/useAsync'
import { formatWhen } from '../lib/dates'
import { formatRupees } from '../lib/money'
import NotFoundPage from './NotFoundPage'

/** One order: its packages (one per seller), items, amounts and the delivery address. */
export default function OrderDetailPage() {
  const { number } = useParams()
  const { data: o, loading, error } = useAsync(() => fetchOrder(number), [number])

  if (loading) return <Spinner label="Loading the order" />
  if (error) {
    const { status, message } = describeError(error)
    return status === 404 ? <NotFoundPage /> : <EmptyState title="Could not load this order" message={message} />
  }

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <Link to="/orders" className="text-sm text-brand-700 hover:underline dark:text-brand-300">‹ My orders</Link>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Order {o.number}</h1>
          <p className="text-sm text-slate-600 dark:text-slate-400">Placed {formatWhen(o.createdAt)}</p>
        </div>
        <div className="flex items-center gap-2">
          <OrderStatusBadge status={o.status} />
          {o.status === 'PENDING_PAYMENT' && <Button size="sm" to="/checkout">Continue to payment</Button>}
        </div>
      </div>

      {o.packages.map((p, i) => (
        <Card key={p.id} className="p-4 sm:p-5">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="flex items-center gap-2 font-semibold text-slate-900 dark:text-white">
              <PackageIcon className="h-5 w-5 text-brand-700 dark:text-brand-300" /> Package {i + 1} · {p.sellerName}
            </h2>
            <PackageStatusBadge status={p.status} />
          </div>
          <ul className="divide-y divide-slate-200 text-sm dark:divide-slate-800">
            {p.items.map((it) => (
              <li key={it.variantId} className="flex justify-between gap-3 py-2">
                <span>
                  <Link to={`/products/${it.productSlug}`} className="font-medium text-slate-900 hover:underline dark:text-white">{it.productName}</Link>
                  <span className="block text-slate-500 dark:text-slate-400">{it.variantLabel} · {formatRupees(it.unitPrice)} × {it.quantity}</span>
                </span>
                <span className="shrink-0 font-medium">{formatRupees(it.lineTotal)}</span>
              </li>
            ))}
          </ul>
          <p className="mt-2 text-right text-sm text-slate-600 dark:text-slate-400">
            {p.discount > 0 && <>Coupon − {formatRupees(p.discount)} · </>}
            Delivery {p.shippingFee > 0 ? formatRupees(p.shippingFee) : 'free'} · <strong className="text-slate-900 dark:text-white">{formatRupees(p.total)}</strong>
          </p>
        </Card>
      ))}

      <div className="grid gap-4 sm:grid-cols-2">
        <Card className="p-4 text-sm">
          <h2 className="mb-2 flex items-center gap-2 font-semibold text-slate-900 dark:text-white"><MapPinIcon className="h-4 w-4" /> Delivery address</h2>
          <address className="not-italic text-slate-600 dark:text-slate-400">
            {o.shipTo.name}<br />{o.shipTo.line1}{o.shipTo.line2 && `, ${o.shipTo.line2}`}<br />
            {o.shipTo.city}, {o.shipTo.state} {o.shipTo.pincode}<br />Phone {o.shipTo.phone}
          </address>
        </Card>
        <Card className="p-4 text-sm">
          <dl className="space-y-1">
            <div className="flex justify-between"><dt>Items</dt><dd>{formatRupees(o.itemsTotal)}</dd></div>
            {o.discountTotal > 0 && <div className="flex justify-between text-green-700 dark:text-green-400"><dt>Coupon {o.couponCode}</dt><dd>− {formatRupees(o.discountTotal)}</dd></div>}
            <div className="flex justify-between"><dt>Delivery</dt><dd>{o.shippingTotal > 0 ? formatRupees(o.shippingTotal) : 'FREE'}</dd></div>
            <div className="flex justify-between border-t border-slate-200 pt-1 font-bold text-slate-900 dark:border-slate-700 dark:text-white"><dt>Total</dt><dd>{formatRupees(o.grandTotal)}</dd></div>
          </dl>
        </Card>
      </div>
    </div>
  )
}
