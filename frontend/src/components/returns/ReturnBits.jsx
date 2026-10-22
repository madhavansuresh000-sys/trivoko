import { formatWhen } from '../../lib/dates'
import { formatRupees } from '../../lib/money'
import { reasonLabel } from '../../lib/returnReasons'
import Badge from '../ui/Badge'

/** Small shared pieces of the return pages (customer, seller, admin). */

const STATUS = {
  REQUESTED: { color: 'amber', label: 'Waiting for the seller' },
  APPROVED: { color: 'brand', label: 'Approved - pickup soon' },
  REJECTED: { color: 'red', label: 'Rejected by the seller' },
  ESCALATED: { color: 'accent', label: 'With TriVoKo for review' },
  PICKED_UP: { color: 'brand', label: 'Picked up' },
  REFUNDED: { color: 'green', label: 'Refunded' },
  REFUND_FAILED: { color: 'red', label: 'Refund delayed - retrying' },
  CLOSED: { color: 'gray', label: 'Closed' },
  CANCELLED: { color: 'gray', label: 'Cancelled' },
}

export function ReturnStatusBadge({ status }) {
  const s = STATUS[status] ?? { color: 'gray', label: status }
  return <Badge color={s.color}>{s.label}</Badge>
}

const WHO = { CUSTOMER: 'You', SELLER: 'Seller', ADMIN: 'TriVoKo', SYSTEM: 'TriVoKo' }

/** "Kovai Run Pro · UK 8 × 1 · order TV-100123 · Kovai Sports · refund ₹2,249" */
export function ReturnHeading({ ret, showShop = true }) {
  return (
    <div>
      <p className="font-semibold text-slate-900 dark:text-white">{ret.productName}</p>
      <p className="text-sm text-slate-600 dark:text-slate-400">
        {ret.variantLabel} × {ret.quantity} · order {ret.orderNumber}{showShop && <> · {ret.sellerName}</>}
      </p>
      <p className="text-sm text-slate-600 dark:text-slate-400">
        {reasonLabel(ret.reason)} · refund <strong className="text-slate-900 dark:text-white">{formatRupees(ret.refundAmount)}</strong>
      </p>
    </div>
  )
}

/** Every move of the return, oldest first, with who made it. `customerView` says "You" for the customer. */
export function ReturnTimeline({ events, customerView = false }) {
  return (
    <ol className="space-y-2 border-l-2 border-slate-200 pl-4 text-sm dark:border-slate-700" aria-label="Return timeline">
      {events.map((e, i) => (
        <li key={i} className="relative">
          <span className="absolute -left-[1.4rem] top-1.5 h-2.5 w-2.5 rounded-full bg-brand-600" aria-hidden="true" />
          <ReturnStatusBadge status={e.status} />
          <span className="ml-2 text-slate-500 dark:text-slate-400">
            {customerView ? WHO[e.actor] : e.actor === 'CUSTOMER' ? 'Customer' : WHO[e.actor]} · {formatWhen(e.at)}
          </span>
          {e.note && <p className="mt-0.5 text-slate-700 dark:text-slate-300">{e.note}</p>}
        </li>
      ))}
    </ol>
  )
}
