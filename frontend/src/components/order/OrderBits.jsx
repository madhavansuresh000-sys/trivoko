import Badge from '../ui/Badge'

/** Small shared pieces of the order pages. */

const ORDER = {
  PENDING_PAYMENT: { color: 'amber', label: 'Waiting for payment' },
  PAID: { color: 'green', label: 'Paid' },
  EXPIRED: { color: 'gray', label: 'Not paid - expired' },
}

const PACKAGE = {
  PENDING_PAYMENT: { color: 'amber', label: 'Waiting for payment' },
  PLACED: { color: 'brand', label: 'Placed - seller is packing' },
  EXPIRED: { color: 'gray', label: 'Expired' },
}

export function OrderStatusBadge({ status }) {
  const s = ORDER[status] ?? { color: 'gray', label: status }
  return <Badge color={s.color}>{s.label}</Badge>
}

export function PackageStatusBadge({ status }) {
  const s = PACKAGE[status] ?? { color: 'gray', label: status }
  return <Badge color={s.color}>{s.label}</Badge>
}
