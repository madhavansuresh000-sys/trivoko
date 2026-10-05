import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link } from 'react-router-dom'

import ProductImage from '../components/catalog/ProductImage'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { CartIcon, MinusIcon, PackageIcon, PlusIcon, TrashIcon } from '../components/ui/icons'
import { Skeleton } from '../components/ui/Loader'
import { formatRupees } from '../lib/money'
import { selectUser } from '../store/authSlice'
import { loadCart, removeFromCart, selectCart, selectCartView, setCartQuantity } from '../store/cartSlice'
import { notify } from '../store/notificationsSlice'

/**
 * The cart (sketch 4): one box per seller = one package, so the order split is visible BEFORE paying.
 * Every number here came from the server (CartPricing); the page only shows it.
 */

const stepper = 'flex h-8 w-8 items-center justify-center rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800'

function CartLine({ line, busy, onQuantity, onRemove }) {
  return (
    <li className="flex gap-3 py-4 first:pt-0 last:pb-0 sm:gap-4">
      <Link to={`/products/${line.productSlug}`} className="w-20 shrink-0 sm:w-24">
        <ProductImage src={line.imageUrl} alt={line.productName} categorySlug="" />
      </Link>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap justify-between gap-x-4">
          <Link to={`/products/${line.productSlug}`}
            className="font-semibold text-slate-900 hover:text-brand-700 dark:text-white dark:hover:text-brand-300">
            {line.productName}
          </Link>
          <p className={`font-semibold ${line.available ? 'text-slate-900 dark:text-white' : 'text-slate-400 line-through'}`}>
            {formatRupees(line.price * line.quantity)}
          </p>
        </div>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          {line.variantLabel} · {formatRupees(line.price)} each
          {line.mrp > line.price && <span className="ml-1 line-through">{formatRupees(line.mrp)}</span>}
        </p>
        {line.note && <p className="mt-1"><Badge color={line.available ? 'amber' : 'red'}>{line.note}</Badge></p>}
        <div className="mt-2 flex items-center gap-2">
          {line.available && (
            <div className="flex items-center gap-2" role="group" aria-label={`Quantity of ${line.productName}`}>
              <button type="button" className={stepper} disabled={busy || line.quantity <= 1}
                onClick={() => onQuantity(line.quantity - 1)} aria-label="One less"><MinusIcon className="h-4 w-4" /></button>
              <span className="w-6 text-center text-sm font-semibold" aria-live="polite">{line.quantity}</span>
              <button type="button" className={stepper} disabled={busy || line.quantity >= line.maxQuantity}
                onClick={() => onQuantity(line.quantity + 1)} aria-label="One more"><PlusIcon className="h-4 w-4" /></button>
            </div>
          )}
          <button type="button" disabled={busy} onClick={onRemove}
            className="ml-auto flex items-center gap-1 rounded-lg px-2 py-1 text-sm font-medium text-slate-600 hover:bg-red-50 hover:text-red-700 sm:ml-2 dark:text-slate-400 dark:hover:bg-red-950/40 dark:hover:text-red-300">
            <TrashIcon className="h-4 w-4" /> Remove
          </button>
        </div>
      </div>
    </li>
  )
}

function PriceDetails({ view, user }) {
  return (
    <Card className="space-y-3 p-5 lg:sticky lg:top-20">
      <h2 className="text-lg font-bold text-slate-900 dark:text-white">Price details</h2>
      <dl className="space-y-2 text-sm">
        <div className="flex justify-between"><dt>Items ({view.itemCount})</dt><dd>{formatRupees(view.itemsTotal)}</dd></div>
        <div className="flex justify-between">
          <dt>Delivery</dt>
          <dd className={view.shippingTotal > 0 ? '' : 'font-semibold text-green-700 dark:text-green-400'}>
            {view.shippingTotal > 0 ? formatRupees(view.shippingTotal) : 'FREE'}
          </dd>
        </div>
        <div className="flex justify-between border-t border-slate-200 pt-2 text-base font-bold text-slate-900 dark:border-slate-700 dark:text-white">
          <dt>Total</dt><dd>{formatRupees(view.total)}</dd>
        </div>
      </dl>
      <Button size="lg" className="w-full" disabled>Checkout</Button>
      <p className="text-xs text-slate-500 dark:text-slate-400">
        Checkout and payment open in the next update. Prices are checked again at checkout.
        {!user && <> <Link to="/login?next=/cart" className="font-medium text-brand-700 hover:underline dark:text-brand-300">Log in</Link> to save this cart.</>}
      </p>
    </Card>
  )
}

export default function CartPage() {
  const dispatch = useDispatch()
  const { status, error } = useSelector(selectCart)
  const view = useSelector(selectCartView)
  const user = useSelector(selectUser)
  const busy = status === 'loading'

  // always show today's prices and stock when the page opens
  useEffect(() => {
    dispatch(loadCart())
  }, [dispatch])

  const act = async (action, done) => {
    try {
      await dispatch(action).unwrap()
      if (done) dispatch(notify(done))
    } catch (e) {
      dispatch(notify(e.message, 'error'))
    }
  }

  const lines = view.packages.reduce((n, p) => n + p.items.length, 0)
  if (lines === 0 && (status === 'loading' || status === 'idle')) {
    return <div className="space-y-4" aria-busy="true"><Skeleton className="h-8 w-48" /><Skeleton className="h-40 w-full" /></div>
  }
  if (lines === 0) {
    return (
      <EmptyState icon={CartIcon} title="Your cart is empty"
        message={status === 'error' ? error : 'Add products from any seller - they all go into this one cart.'}
        action={<Button to="/products">Start shopping</Button>} />
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
      <div className="space-y-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Your cart ({view.itemCount} {view.itemCount === 1 ? 'item' : 'items'})</h1>
          <p className="text-sm text-slate-600 dark:text-slate-400">
            This order comes in <strong>{view.packages.length} {view.packages.length === 1 ? 'package' : 'packages'}</strong>
            {view.packages.length > 1 && ' - one from each seller'}.
          </p>
        </div>
        {view.packages.map((pkg, i) => (
          <Card key={pkg.seller.slug} as="section" className="p-4 sm:p-5" aria-label={`Package ${i + 1} from ${pkg.seller.name}`}>
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h2 className="flex items-center gap-2 font-semibold text-slate-900 dark:text-white">
                <PackageIcon className="h-5 w-5 text-brand-700 dark:text-brand-300" />
                Package {i + 1} · <Link to={`/shops/${pkg.seller.slug}`} className="hover:underline">{pkg.seller.name}</Link>
              </h2>
              {pkg.items.some((l) => l.available) && (
                pkg.shippingFee > 0
                  ? <Badge color="amber">Delivery {formatRupees(pkg.shippingFee)} · free from ₹499</Badge>
                  : <Badge color="green">Free delivery</Badge>
              )}
            </div>
            <ul className="divide-y divide-slate-200 dark:divide-slate-800">
              {pkg.items.map((line) => (
                <CartLine key={line.variantId} line={line} busy={busy}
                  onQuantity={(q) => act(setCartQuantity({ variantId: line.variantId, quantity: q }))}
                  onRemove={() => act(removeFromCart(line.variantId), `Removed ${line.productName}`)} />
              ))}
            </ul>
          </Card>
        ))}
      </div>
      <div><PriceDetails view={view} user={user} /></div>
    </div>
  )
}
