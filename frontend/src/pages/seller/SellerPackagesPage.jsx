import { useState } from 'react'

import { fetchSellerPackages } from '../../api/seller'
import PackageCard from '../../components/seller/PackageCard'
import EmptyState from '../../components/ui/EmptyState'
import { PackageIcon } from '../../components/ui/icons'
import { Skeleton } from '../../components/ui/Loader'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'

/** "Orders to ship": my packages, filtered by status, newest first. */
const TABS = [
  { value: '', label: 'All' },
  { value: 'PLACED', label: 'To pack' },
  { value: 'PACKED', label: 'To ship' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

export default function SellerPackagesPage() {
  const [status, setStatus] = useState('PLACED')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const { data, loading, error } = useAsync(() => fetchSellerPackages(status, page), [status, page, reload])

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Orders to ship</h1>
      <div className="-mx-1 flex gap-1 overflow-x-auto px-1" role="tablist" aria-label="Package status">
        {TABS.map((t) => (
          <button key={t.value} type="button" role="tab" aria-selected={status === t.value}
            onClick={() => { setStatus(t.value); setPage(0) }}
            className={`shrink-0 rounded-full px-3 py-1.5 text-sm font-medium ${status === t.value
              ? 'bg-brand-700 text-white' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'}`}>
            {t.label}
          </button>
        ))}
      </div>
      {loading ? <div className="space-y-3"><Skeleton className="h-24" /><Skeleton className="h-24" /></div>
        : error ? <EmptyState title="Could not load your packages" message={describeError(error).message} />
          : data.content.length === 0 ? (
            <EmptyState icon={PackageIcon} title="Nothing here" message="When customers buy from your shop, their packages show up here." />
          ) : (
            <ul className="space-y-3">
              {data.content.map((p) => <li key={p.id}><PackageCard pkg={p} onChanged={() => setReload((r) => r + 1)} /></li>)}
            </ul>
          )}
      {data && data.totalPages > 1 && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  )
}
