import { formatWhen } from '../../lib/dates'
import { CheckIcon } from '../ui/icons'

/**
 * The tracking timeline of ONE package (like a courier app):
 *   ● Placed 10:42  ● Packed 12:00  ● Shipped 15:10 (AWB12345)  ○ Delivered
 * Done steps come from the server's events; the rest of the normal route is shown as "next" (grey).
 * A cancelled or expired package simply ends there.
 */
const ROUTE = ['PLACED', 'PACKED', 'SHIPPED', 'DELIVERED']
const LABEL = { PLACED: 'Placed', PACKED: 'Packed', SHIPPED: 'Shipped', DELIVERED: 'Delivered', CANCELLED: 'Cancelled', EXPIRED: 'Not paid - expired' }

export default function PackageTimeline({ status, events = [], trackingNumber }) {
  const done = events.filter((e) => e.status !== 'PENDING_PAYMENT')
  const ended = status === 'CANCELLED' || status === 'EXPIRED' || status === 'DELIVERED'
  const next = ended ? [] : ROUTE.slice(ROUTE.indexOf(status) + 1)

  return (
    <ol className="space-y-2" aria-label="Package tracking">
      {done.map((e) => {
        const bad = e.status === 'CANCELLED' || e.status === 'EXPIRED'
        return (
          <li key={e.status + e.at} data-state="done" className="flex gap-3">
            <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${bad
              ? 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300' : 'bg-brand-700 text-white'}`}>
              {bad ? '×' : <CheckIcon className="h-3.5 w-3.5" />}
            </span>
            <span className="text-sm">
              <span className="font-semibold text-slate-900 dark:text-white">{LABEL[e.status] ?? e.status}</span>
              <span className="ml-2 text-xs text-slate-500 dark:text-slate-400">{formatWhen(e.at)}</span>
              {e.status === 'SHIPPED' && trackingNumber && <span className="block text-xs text-slate-600 dark:text-slate-400">Tracking number (AWB): {trackingNumber}</span>}
              {e.status === 'CANCELLED' && e.note && <span className="block text-xs text-slate-600 dark:text-slate-400">{e.note}</span>}
            </span>
          </li>
        )
      })}
      {next.map((s) => (
        <li key={s} data-state="next" className="flex gap-3">
          <span className="mt-0.5 h-5 w-5 shrink-0 rounded-full border-2 border-slate-300 dark:border-slate-600" />
          <span className="text-sm text-slate-400 dark:text-slate-500">{LABEL[s]}</span>
        </li>
      ))}
    </ol>
  )
}
