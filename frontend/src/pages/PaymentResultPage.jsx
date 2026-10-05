import { useEffect } from 'react'
import { useDispatch } from 'react-redux'
import { Link, useSearchParams } from 'react-router-dom'

import { fetchOrder, verifyPayment } from '../api/orders'
import { OrderStatusBadge } from '../components/order/OrderBits'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import { CheckIcon, PackageIcon } from '../components/ui/icons'
import { Spinner } from '../components/ui/Loader'
import useAsync, { describeError } from '../hooks/useAsync'
import { formatRupees } from '../lib/money'
import { loadCart } from '../store/cartSlice'

/**
 * Back from paying.
 *   /payment/success?order=TV-100123[&session=cs_...]  - with a Stripe session we ask the server to check with
 *                                                        Stripe now (the webhook may arrive a few seconds later)
 *   /payment/cancelled?order=TV-100123                 - nothing was paid; the items stay held for 10 minutes
 */
export default function PaymentResultPage({ cancelled = false }) {
  const [params] = useSearchParams()
  const dispatch = useDispatch()
  const number = params.get('order')
  const session = params.get('session')
  const { data: order, loading, error } = useAsync(
    () => (session && !cancelled ? verifyPayment(session) : fetchOrder(number)), [number, session, cancelled])

  // the bought items have left the saved cart: refresh the navbar count
  useEffect(() => {
    if (order?.status === 'PAID') dispatch(loadCart())
  }, [order?.status, dispatch])

  if (loading) return <Spinner label="Checking your payment" />
  if (error) return <Card className="mx-auto max-w-md p-6 text-center">{describeError(error).message}</Card>

  if (cancelled || order.status !== 'PAID') {
    return (
      <Card className="mx-auto max-w-md space-y-4 p-6 text-center">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Payment not completed</h1>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          Order {order.number} is <OrderStatusBadge status={order.status} />.
          {order.status === 'PENDING_PAYMENT' && ' Your items are held for a few more minutes.'}
        </p>
        <div className="flex flex-wrap justify-center gap-2">
          {order.status === 'PENDING_PAYMENT' && <Button to="/checkout">Try paying again</Button>}
          <Button variant="secondary" to="/cart">Back to cart</Button>
        </div>
      </Card>
    )
  }

  return (
    <Card className="mx-auto max-w-lg space-y-5 p-6 text-center">
      <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300">
        <CheckIcon className="h-8 w-8" />
      </span>
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Order {order.number} placed!</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          You paid {formatRupees(order.grandTotal)} once. It comes in {order.packages.length} {order.packages.length === 1 ? 'package' : 'packages'}:
        </p>
      </div>
      <ul className="space-y-2 text-left text-sm">
        {order.packages.map((p) => (
          <li key={p.id} className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 dark:border-slate-700">
            <PackageIcon className="h-4 w-4 text-brand-700 dark:text-brand-300" />
            <span className="flex-1">{p.sellerName} packs {p.items.reduce((n, i) => n + i.quantity, 0)} item(s)</span>
            <span className="font-medium">{formatRupees(p.total)}</span>
          </li>
        ))}
      </ul>
      <div className="flex flex-wrap justify-center gap-2">
        <Button to={`/orders/${order.number}`}>View order</Button>
        <Button variant="secondary" to="/products">Keep shopping</Button>
      </div>
      <p className="text-xs text-slate-500 dark:text-slate-400">
        <Link to="/orders" className="underline">My orders</Link> shows every order and its packages.
      </p>
    </Card>
  )
}
