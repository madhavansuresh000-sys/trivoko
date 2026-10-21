import { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { Link } from 'react-router-dom'

import { fetchAddresses } from '../api/account'
import { placeOrder, previewCheckout } from '../api/orders'
import { Notice } from '../components/auth/AuthCard'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { CartIcon, CheckIcon, PackageIcon } from '../components/ui/icons'
import { Skeleton } from '../components/ui/Loader'
import useAsync, { describeError } from '../hooks/useAsync'
import { formatRupees } from '../lib/money'
import { selectCartLines } from '../store/cartSlice'

/**
 * Checkout (sketch 5): address -> review packages -> coupon -> Pay.
 * Every amount comes from POST /api/checkout/preview (the server recalculates prices, coupon shares and
 * delivery). "Pay" sends the total the customer SAW; if prices changed meanwhile the server says 409 and the
 * page shows the new numbers before anyone pays.
 */

function AddressChoice({ addresses, value, onChange }) {
  if (addresses.length === 0) {
    return (
      <Notice tone="error">
        You have no delivery address yet. <Link to="/account/addresses" className="font-semibold underline">Add an address</Link> first.
      </Notice>
    )
  }
  return (
    <fieldset className="space-y-2">
      <legend className="sr-only">Delivery address</legend>
      {addresses.map((a) => (
        <label key={a.id} className={`flex cursor-pointer gap-3 rounded-xl border p-3 text-sm ${value === a.id
          ? 'border-brand-600 bg-brand-50 dark:bg-slate-800' : 'border-slate-200 dark:border-slate-700'}`}>
          <input type="radio" name="address" className="mt-1 accent-brand-700" checked={value === a.id} onChange={() => onChange(a.id)} />
          <span>
            <span className="font-semibold text-slate-900 dark:text-white">{a.name}</span>
            {a.isDefault && <Badge color="brand" className="ml-2">Default</Badge>}
            <span className="block text-slate-600 dark:text-slate-400">
              {a.line1}{a.line2 && `, ${a.line2}`}, {a.city} {a.pincode} · {a.phone}
            </span>
          </span>
        </label>
      ))}
      <Link to="/account/addresses" className="inline-block text-sm font-medium text-brand-700 hover:underline dark:text-brand-300">
        + Add or edit addresses
      </Link>
    </fieldset>
  )
}

/** "Price changed: Silicone case is now Rs 499 (was Rs 549)" - compared with the cart the customer last saw. */
function priceChanges(preview, cartLines) {
  const seen = new Map(cartLines.map((l) => [l.variantId, Number(l.price)]))
  return preview.packages.flatMap((p) => p.items)
    .filter((i) => seen.has(i.variantId) && seen.get(i.variantId) !== Number(i.unitPrice))
    .map((i) => `Price changed: ${i.productName} is now ${formatRupees(i.unitPrice)} (was ${formatRupees(seen.get(i.variantId))})`)
}

function Packages({ preview }) {
  return (
    <div className="space-y-3">
      {preview.packages.map((p, i) => (
        <div key={p.sellerSlug ?? p.sellerName} className="rounded-xl border border-slate-200 p-3 dark:border-slate-700">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="flex items-center gap-2 font-semibold text-slate-900 dark:text-white">
              <PackageIcon className="h-4 w-4 text-brand-700 dark:text-brand-300" /> Package {i + 1} · {p.sellerName}
            </p>
            {p.shippingFee > 0 ? <Badge color="amber">Delivery {formatRupees(p.shippingFee)}</Badge> : <Badge color="green">Free delivery</Badge>}
          </div>
          <ul className="mt-2 space-y-1 text-sm">
            {p.items.map((it) => (
              <li key={it.variantId} className="flex justify-between gap-3">
                <span className="text-slate-700 dark:text-slate-300">{it.productName} <span className="text-slate-500">({it.variantLabel}) × {it.quantity}</span></span>
                <span className="shrink-0 font-medium">{formatRupees(it.lineTotal)}</span>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  )
}

function CouponBox({ applied, result, onApply, onRemove, busy }) {
  const [code, setCode] = useState(applied ?? '')
  return (
    <div className="space-y-2">
      <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); if (code.trim()) onApply(code.trim().toUpperCase()) }}>
        <label htmlFor="coupon" className="sr-only">Coupon code</label>
        <input id="coupon" value={code} onChange={(e) => setCode(e.target.value)} placeholder="Coupon code (e.g. WELCOME10)"
          autoCapitalize="characters"
          className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm uppercase dark:border-slate-700 dark:bg-slate-950" />
        <Button type="submit" variant="secondary" disabled={busy || !code.trim()}>Apply</Button>
      </form>
      {result && (result.error
        ? <p className="text-sm text-red-700 dark:text-red-400">{result.error}</p>
        : (
          <p className="flex items-center gap-1 text-sm text-green-700 dark:text-green-400">
            <CheckIcon className="h-4 w-4" /> {result.code} applied: you save {formatRupees(result.discount)}
            <button type="button" className="ml-auto text-xs font-medium text-slate-500 underline" onClick={() => { setCode(''); onRemove() }}>Remove</button>
          </p>
        ))}
    </div>
  )
}

export default function CheckoutPage() {
  const cartLines = useSelector(selectCartLines)
  const addresses = useAsync(() => fetchAddresses(), [])
  const [addressId, setAddressId] = useState(null)
  const [coupon, setCoupon] = useState(undefined)
  const [reload, setReload] = useState(0)
  const [placing, setPlacing] = useState(false)
  const [placeError, setPlaceError] = useState(null)

  // default address first; the customer can pick another
  const chosen = addressId ?? addresses.data?.find((a) => a.isDefault)?.id ?? addresses.data?.[0]?.id ?? null
  const preview = useAsync(() => previewCheckout({ addressId: chosen ?? undefined, couponCode: coupon }), [chosen, coupon, reload])

  // the browser tab title tells which step you are on (sketch: Address -> Review -> Pay)
  useEffect(() => {
    document.title = 'Checkout - TriVoKo'
  }, [])

  const pay = async () => {
    setPlacing(true)
    setPlaceError(null)
    try {
      const placed = await placeOrder({ addressId: chosen, couponCode: coupon, expectedTotal: preview.data.grandTotal })
      window.location.assign(placed.redirectUrl) // Stripe's page, or our own test page
    } catch (e) {
      setPlaceError(describeError(e).message) // e.g. "Prices changed ..." or "only 1 left"
      setReload((r) => r + 1) // show today's numbers
      setPlacing(false)
    }
  }

  if (preview.loading && !preview.data) {
    return <div className="grid gap-6 lg:grid-cols-[1fr_22rem]" aria-busy="true"><Skeleton className="h-72" /><Skeleton className="h-72" /></div>
  }
  if (preview.error) {
    return <EmptyState title="Could not load checkout" message={describeError(preview.error).message} action={<Button to="/cart">Back to cart</Button>} />
  }
  const p = preview.data
  if (p.packages.length === 0) {
    return (
      <EmptyState icon={CartIcon} title={p.unavailable.length ? 'Nothing in your cart can be bought right now' : 'Your cart is empty'}
        message={p.unavailable.length ? `Not available: ${p.unavailable.join(', ')}` : 'Add something first - then come back here to pay.'}
        action={<Button to="/products">Start shopping</Button>} />
    )
  }
  const changes = priceChanges(p, cartLines)
  const canPay = chosen !== null && !placing && !(p.coupon && p.coupon.error)

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
      <div className="space-y-4">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Checkout</h1>
        <Card className="space-y-3 p-5">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-white">1. Deliver to</h2>
          {addresses.loading ? <Skeleton className="h-16" /> : <AddressChoice addresses={addresses.data ?? []} value={chosen} onChange={setAddressId} />}
        </Card>
        <Card className="space-y-3 p-5">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-white">2. Review packages</h2>
          <p className="text-sm text-slate-600 dark:text-slate-400">
            You pay once; each seller packs and ships their own package.
          </p>
          <Packages preview={p} />
          {changes.map((c) => <Notice key={c} tone="error">⚠ {c}</Notice>)}
          {p.unavailable.length > 0 && <Notice tone="error">Left out (cannot be bought now): {p.unavailable.join(', ')}</Notice>}
        </Card>
      </div>

      <Card className="h-fit space-y-4 p-5 lg:sticky lg:top-20">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white">3. Pay</h2>
        <CouponBox applied={coupon} result={p.coupon} busy={preview.loading} onApply={setCoupon} onRemove={() => setCoupon(undefined)} />
        <dl className="space-y-2 border-t border-slate-200 pt-3 text-sm dark:border-slate-700">
          <div className="flex justify-between"><dt>Items</dt><dd>{formatRupees(p.itemsTotal)}</dd></div>
          {p.discountTotal > 0 && <div className="flex justify-between text-green-700 dark:text-green-400"><dt>Coupon</dt><dd>− {formatRupees(p.discountTotal)}</dd></div>}
          <div className="flex justify-between"><dt>Delivery</dt><dd>{p.shippingTotal > 0 ? formatRupees(p.shippingTotal) : 'FREE'}</dd></div>
          <div className="flex justify-between border-t border-slate-200 pt-2 text-base font-bold text-slate-900 dark:border-slate-700 dark:text-white">
            <dt>To pay</dt><dd>{formatRupees(p.grandTotal)}</dd>
          </div>
        </dl>
        {placeError && <Notice tone="error">{placeError}</Notice>}
        <Button size="lg" className="w-full" disabled={!canPay} onClick={pay}>
          {placing ? 'Opening payment…' : `Pay ${formatRupees(p.grandTotal)}`}
        </Button>
        <p className="text-xs text-slate-500 dark:text-slate-400">Your items are held for 10 minutes while you pay.</p>
      </Card>
    </div>
  )
}
