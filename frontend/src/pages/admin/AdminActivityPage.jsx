import { useState } from 'react'

import { fetchActivity } from '../../api/admin'
import { PageTitle } from '../../components/admin/AdminBits'
import Card from '../../components/ui/Card'
import { SelectField } from '../../components/ui/FormField'
import { Skeleton } from '../../components/ui/Loader'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'

/** The audit log: who changed what and when, newest first. Lines are never edited or deleted. */
const ACTIONS = ['SELLER_APPROVED', 'SELLER_REJECTED', 'SELLER_BLOCKED', 'SELLER_UNBLOCKED', 'PRODUCT_APPROVED',
  'PRODUCT_REJECTED', 'USER_BLOCKED', 'USER_UNBLOCKED', 'COUPON_CREATED', 'COUPON_UPDATED', 'COUPON_ENDED', 'PRICE_CHANGED']

const label = (action) => action.replaceAll('_', ' ').toLowerCase()

export default function AdminActivityPage() {
  const [action, setAction] = useState('')
  const [page, setPage] = useState(0)
  const { data, loading, error } = useAsync(() => fetchActivity(action, page), [action, page])

  return (
    <div className="space-y-4">
      <PageTitle right={
        <SelectField id="activity-action" label="Show" className="w-full max-w-xs" value={action} placeholder="Everything"
          options={ACTIONS.map((a) => ({ value: a, label: label(a) }))} onChange={(e) => { setAction(e.target.value); setPage(0) }} />
      }>Activity log</PageTitle>

      {loading ? <Skeleton className="h-40" /> : error ? <Card className="p-4">{describeError(error).message}</Card>
        : data.content.length === 0 ? <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">Nothing here yet.</Card> : (
          <Card as="ol" className="divide-y divide-slate-200 dark:divide-slate-800">
            {data.content.map((l) => (
              <li key={l.id} className="px-4 py-3 text-sm">
                <p className="text-slate-900 dark:text-white">
                  <span className="font-semibold capitalize">{label(l.action)}</span>
                  <span className="text-slate-600 dark:text-slate-400"> · {l.entityType.toLowerCase()} #{l.entityId}</span>
                </p>
                {l.details && <p className="break-words text-slate-700 dark:text-slate-300">{l.details}</p>}
                <p className="text-xs text-slate-500 dark:text-slate-400">{l.actorEmail} · {formatWhen(l.createdAt)}</p>
              </li>
            ))}
          </Card>
        )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  )
}
