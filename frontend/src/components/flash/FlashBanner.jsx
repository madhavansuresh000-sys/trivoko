import Badge from '../ui/Badge'
import Button from '../ui/Button'
import { BoltIcon } from '../ui/icons'
import useAsync from '../../hooks/useAsync'
import useCountdown from '../../hooks/useCountdown'
import { fetchCurrentSale } from '../../api/flash'
import { formatClock } from '../../lib/countdown'

/**
 * The home-page banner. A live or upcoming sale shows its countdown (server time) and a button to the sale page;
 * with no sale it shows the normal welcome banner.
 */
function SaleBanner({ sale }) {
  const live = sale.state === 'LIVE'
  const left = useCountdown(live ? sale.endsAt : sale.startsAt, sale.serverNow)
  return (
    <section className="relative overflow-hidden rounded-3xl bg-brand-800 px-6 py-10 text-white sm:px-10">
      <Badge color="accent"><BoltIcon className="mr-1 h-3.5 w-3.5" /> {live ? 'Flash sale is live' : 'Flash sale coming'}</Badge>
      <h1 className="mt-4 max-w-xl text-3xl font-extrabold tracking-tight sm:text-4xl">{sale.title}</h1>
      <p className="mt-3 flex items-center gap-3 text-brand-100">
        {live ? 'Ends in' : 'Starts in'}
        <span className="font-mono text-3xl font-extrabold tabular-nums text-accent-300">{formatClock(left)}</span>
      </p>
      <div className="mt-6 flex flex-wrap gap-3">
        <Button variant="deal" size="lg" to="/flash">{live ? 'Grab the deals' : 'See the deals'}</Button>
        <Button variant="light" size="lg" to="/products">Keep shopping</Button>
      </div>
      <span aria-hidden="true" className="pointer-events-none absolute -bottom-6 right-4 hidden text-[9rem] leading-none opacity-20 sm:block">⚡</span>
    </section>
  )
}

export default function FlashBanner({ fallback }) {
  const { data } = useAsync(() => fetchCurrentSale().catch(() => null), [])
  return data ? <SaleBanner sale={data} /> : fallback
}
