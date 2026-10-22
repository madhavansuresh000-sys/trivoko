import { useCallback, useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, useNavigate } from 'react-router-dom'

import { claimDeal, fetchCurrentSale, fetchMyClaims, fetchSale } from '../api/flash'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { BoltIcon } from '../components/ui/icons'
import { Skeleton } from '../components/ui/Loader'
import { describeError } from '../hooks/useAsync'
import useCountdown from '../hooks/useCountdown'
import { formatClock } from '../lib/countdown'
import { formatRupees } from '../lib/money'
import { selectUser } from '../store/authSlice'
import { notify } from '../store/notificationsSlice'

/**
 * The flash-sale page: a countdown (server time), the deals with a live "N left" (asked again every 3 seconds),
 * and a Claim button. A claim keeps the unit for 10 minutes: pay from the checkout before the timer ends.
 */
const POLL_MS = 3000

function Clock({ sale }) {
  const target = sale.state === 'UPCOMING' ? sale.startsAt : sale.endsAt
  const left = useCountdown(target, sale.serverNow)
  return (
    <span className="font-mono text-2xl font-extrabold tabular-nums" aria-live="off">
      {formatClock(left)}
    </span>
  )
}

function Deal({ deal, live, claimed, busy, onClaim }) {
  const off = deal.mrp > 0 ? Math.round(((deal.mrp - deal.salePrice) / deal.mrp) * 100) : 0
  const lowStock = deal.left > 0 && deal.left <= Math.max(3, Math.ceil(deal.quantity * 0.1))
  return (
    <Card className="flex flex-col overflow-hidden">
      <div className="flex h-40 items-center justify-center bg-slate-100 dark:bg-slate-800">
        {deal.imageUrl ? <img src={deal.imageUrl} alt="" className="h-full w-full object-cover" loading="lazy" />
          : <span aria-hidden="true" className="text-5xl">⚡</span>}
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <Link to={`/products/${deal.productSlug}`} className="font-semibold text-slate-900 hover:underline dark:text-white">
          {deal.productName}
        </Link>
        <p className="text-sm text-slate-600 dark:text-slate-400">{deal.variantLabel} · {deal.shopName}</p>
        <p className="flex items-baseline gap-2">
          <span className="text-2xl font-extrabold text-slate-900 dark:text-white">{formatRupees(deal.salePrice)}</span>
          <span className="text-sm text-slate-500 line-through">{formatRupees(deal.mrp)}</span>
          {off > 0 && <Badge color="accent">{off}% off</Badge>}
        </p>
        <p role="status" className={`text-sm font-medium ${deal.soldOut ? 'text-red-700 dark:text-red-400' : lowStock ? 'text-amber-700 dark:text-amber-300' : 'text-slate-600 dark:text-slate-400'}`}>
          {deal.soldOut ? 'Sold out' : `${deal.left} of ${deal.quantity} left`}
        </p>
        <div className="mt-auto pt-2">
          {claimed ? (
            <Button to="/checkout" variant="deal" className="w-full">Yours! Go to checkout</Button>
          ) : (
            <Button variant="deal" className="w-full" disabled={!live || deal.soldOut || busy} onClick={() => onClaim(deal)}>
              {deal.soldOut ? 'Sold out' : live ? (busy ? 'Claiming…' : 'Claim now') : 'Not started'}
            </Button>
          )}
        </div>
      </div>
    </Card>
  )
}

function MyDeals({ claims }) {
  const held = claims.filter((c) => c.status === 'HELD' || c.status === 'ORDERED')
  if (held.length === 0) return null
  return (
    <Card className="border-accent-400 bg-accent-50 p-4 dark:bg-accent-950/30">
      <h2 className="font-bold text-slate-900 dark:text-white">Your deals are waiting</h2>
      <ul className="mt-2 space-y-1 text-sm text-slate-700 dark:text-slate-300">
        {held.map((c) => <HeldLine key={c.claimId} claim={c} />)}
      </ul>
      <Button to="/checkout" variant="deal" className="mt-3">Pay now at checkout</Button>
    </Card>
  )
}

function HeldLine({ claim }) {
  const left = useCountdown(claim.expiresAt, claim.serverNow)
  return (
    <li>
      {claim.productName} ({claim.variantLabel}) at {formatRupees(claim.salePrice)} ·{' '}
      {claim.status === 'HELD' ? <>held for <strong className="tabular-nums">{formatClock(left)}</strong></> : 'in your unpaid order'}
    </li>
  )
}

export default function FlashSalePage() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector(selectUser)
  const [sale, setSale] = useState(null)
  const [state, setState] = useState('loading') // loading | none | ready | error
  const [claims, setClaims] = useState([])
  const [busyItem, setBusyItem] = useState(null)

  const loadClaims = useCallback(() => {
    if (user) fetchMyClaims().then(setClaims).catch(() => {})
  }, [user])

  // first load: the current sale; then ask for fresh numbers every few seconds
  useEffect(() => {
    let alive = true
    fetchCurrentSale()
      .then((s) => { if (alive) { setSale(s); setState(s ? 'ready' : 'none') } })
      .catch(() => alive && setState('error'))
    return () => { alive = false }
  }, [])

  const saleId = sale?.id
  useEffect(() => {
    if (!saleId) return undefined
    const timer = setInterval(() => {
      fetchSale(saleId).then(setSale).catch(() => {})
    }, POLL_MS)
    return () => clearInterval(timer)
  }, [saleId])

  useEffect(loadClaims, [loadClaims])

  const claim = async (deal) => {
    if (!user) {
      navigate('/login?next=/flash')
      return
    }
    setBusyItem(deal.itemId)
    try {
      await claimDeal(deal.itemId)
      dispatch(notify(`${deal.productName} is yours for 10 minutes - pay at checkout`))
      loadClaims()
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    } finally {
      setBusyItem(null)
      if (saleId) fetchSale(saleId).then(setSale).catch(() => {}) // show the true "left" at once
    }
  }

  if (state === 'loading') return <Skeleton className="h-64" />
  if (state === 'error') return <EmptyState title="Could not load the sale" text="Is the backend running? Try again in a moment." />
  if (state === 'none') {
    return <EmptyState title="No flash sale right now" text="Sales are announced here and on the home page. Come back soon!" action={<Button to="/products">Browse products</Button>} />
  }

  const live = sale.state === 'LIVE'
  const claimedItems = new Set(claims.map((c) => c.itemId))
  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-brand-800 px-6 py-8 text-white">
        <Badge color="accent"><BoltIcon className="mr-1 h-3.5 w-3.5" /> Flash sale</Badge>
        <h1 className="mt-3 text-3xl font-extrabold">{sale.title}</h1>
        <p className="mt-2 flex items-center gap-3 text-brand-100">
          {sale.state === 'UPCOMING' ? 'Starts in' : sale.state === 'LIVE' ? 'Ends in' : 'This sale has ended'}
          {sale.state !== 'ENDED' && <Clock sale={sale} />}
        </p>
        <p className="mt-1 text-sm text-brand-100">One unit per person. A claimed unit is kept for 10 minutes while you pay.</p>
      </section>

      <MyDeals claims={claims} />

      {sale.items.length === 0 ? (
        <EmptyState title="No deals added yet" text="Sellers are still adding their deals. Check back before the sale starts." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {sale.items.map((d) => (
            <Deal key={d.itemId} deal={d} live={live} claimed={claimedItems.has(d.itemId)} busy={busyItem === d.itemId} onClaim={claim} />
          ))}
        </div>
      )}
    </div>
  )
}
