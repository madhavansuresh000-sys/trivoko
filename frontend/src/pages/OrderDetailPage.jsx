import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { Link, useParams } from 'react-router-dom'

import { cancelOrderPackage, fetchOrder } from '../api/orders'
import { fetchMyReturns } from '../api/returns'
import { OrderStatusBadge, PackageStatusBadge } from '../components/order/OrderBits'
import PackageTimeline from '../components/order/PackageTimeline'
import { ReturnStatusBadge } from '../components/returns/ReturnBits'
import ReturnRequestDialog from '../components/returns/ReturnRequestDialog'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { MapPinIcon, PackageIcon } from '../components/ui/icons'
import { Spinner } from '../components/ui/Loader'
import Modal from '../components/ui/Modal'
import useAsync, { describeError } from '../hooks/useAsync'
import { formatWhen } from '../lib/dates'
import { formatRupees } from '../lib/money'
import { notify } from '../store/notificationsSlice'
import NotFoundPage from './NotFoundPage'

/** A delivered item may be returned for 7 days (the server checks again). */
const RETURN_DAYS = 7
const canStillReturn = (pkg) =>
  pkg.status === 'DELIVERED' && pkg.deliveredAt && Date.now() - new Date(pkg.deliveredAt).getTime() < RETURN_DAYS * 86400000

/** One order: its packages (one per seller), items, amounts and the delivery address. */
export default function OrderDetailPage() {
  const { number } = useParams()
  const dispatch = useDispatch()
  const [reload, setReload] = useState(0)
  const { data: o, loading, error } = useAsync(() => fetchOrder(number), [number, reload])
  const [cancelling, setCancelling] = useState(null) // the package being cancelled (asks first)
  const [busy, setBusy] = useState(false)
  const [returning, setReturning] = useState(null) // the item in the "Return this item" dialog
  // which lines already have a return (one per line); a failure here only hides the badges
  const { data: myReturns } = useAsync(() => fetchMyReturns().catch(() => []), [reload])
  const returnOf = (itemId) => (myReturns ?? []).find((r) => r.orderItemId === itemId)

  const returnAsked = () => {
    setReturning(null)
    dispatch(notify('Return requested - the seller has 2 days to answer.'))
    setReload((r) => r + 1)
  }

  const confirmCancel = async () => {
    setBusy(true)
    try {
      await cancelOrderPackage(number, cancelling.id)
      dispatch(notify(`Package cancelled - ${formatRupees(cancelling.total)} is refunded to your payment method.`))
      setReload((r) => r + 1)
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    } finally {
      setBusy(false)
      setCancelling(null)
    }
  }

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
          {o.status === 'PAID' && (
            // a plain link: the browser sends the login cookie and downloads the file itself
            <a href={`/api/orders/${encodeURIComponent(o.number)}/invoice.pdf`}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800">
              Download invoice (PDF)
            </a>
          )}
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
                <span className="flex shrink-0 flex-col items-end gap-1">
                  <span className="font-medium">{formatRupees(it.lineTotal)}</span>
                  {returnOf(it.id) ? (
                    <Link to={`/account/returns/${returnOf(it.id).id}`} aria-label={`Return of ${it.productName}`}>
                      <ReturnStatusBadge status={returnOf(it.id).status} />
                    </Link>
                  ) : canStillReturn(p) && (
                    <Button size="sm" variant="ghost" onClick={() => setReturning(it)}>Return</Button>
                  )}
                </span>
              </li>
            ))}
          </ul>
          {p.status !== 'PENDING_PAYMENT' && (
            <div className="mt-3 border-t border-slate-200 pt-3 dark:border-slate-800">
              <PackageTimeline status={p.status} events={p.events} trackingNumber={p.trackingNumber} />
            </div>
          )}
          {(p.status === 'PLACED' || p.status === 'PACKED') && (
            <div className="mt-3 text-right">
              <Button size="sm" variant="ghost" onClick={() => setCancelling(p)}>
                <span className="text-red-700 dark:text-red-400">Cancel this package</span>
              </Button>
            </div>
          )}
          <p className="mt-2 text-right text-sm text-slate-600 dark:text-slate-400">
            {p.discount > 0 && <>Coupon − {formatRupees(p.discount)} · </>}
            Delivery {p.shippingFee > 0 ? formatRupees(p.shippingFee) : 'free'} · <strong className="text-slate-900 dark:text-white">{formatRupees(p.total)}</strong>
          </p>
        </Card>
      ))}

      <Modal open={cancelling !== null} onClose={() => setCancelling(null)} title="Cancel this package?"
        footer={<>
          <Button variant="secondary" onClick={() => setCancelling(null)}>Keep it</Button>
          <Button variant="danger" disabled={busy} onClick={confirmCancel}>{busy ? 'Cancelling…' : 'Cancel package'}</Button>
        </>}>
        <p>{cancelling?.sellerName} will not ship it, and {cancelling ? formatRupees(cancelling.total) : ''} goes back to your payment method. Your other packages are not affected.</p>
      </Modal>

      <ReturnRequestDialog item={returning} onClose={() => setReturning(null)} onDone={returnAsked} />

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
