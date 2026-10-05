import { useState } from 'react'
import { Link } from 'react-router-dom'

import { fetchMyProducts } from '../../api/seller'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import EmptyState from '../../components/ui/EmptyState'
import { PlusIcon } from '../../components/ui/icons'
import { Skeleton } from '../../components/ui/Loader'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatRupees } from '../../lib/money'

/** My products with their approval status; "New product" opens the editor. */
const STATUS = {
  DRAFT: { color: 'gray', label: 'Draft' },
  PENDING: { color: 'amber', label: 'Waiting for approval' },
  ACTIVE: { color: 'green', label: 'Live' },
  REJECTED: { color: 'red', label: 'Not approved' },
  BLOCKED: { color: 'red', label: 'Blocked' },
}

const FILTERS = [['', 'All'], ['ACTIVE', 'Live'], ['DRAFT', 'Drafts'], ['PENDING', 'Waiting'], ['REJECTED', 'Not approved']]

export default function SellerProductsPage() {
  const [status, setStatus] = useState('')
  const { data, loading, error } = useAsync(() => fetchMyProducts(status), [status])

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">My products</h1>
        <Button to="/seller/products/new"><PlusIcon className="h-4 w-4" /> New product</Button>
      </div>
      <div className="flex gap-1 overflow-x-auto" role="tablist" aria-label="Product status">
        {FILTERS.map(([value, label]) => (
          <button key={value} type="button" role="tab" aria-selected={status === value} onClick={() => setStatus(value)}
            className={`shrink-0 rounded-full px-3 py-1.5 text-sm font-medium ${status === value ? 'bg-brand-700 text-white' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'}`}>
            {label}
          </button>
        ))}
      </div>
      {loading ? <Skeleton className="h-40" />
        : error ? <EmptyState title="Could not load your products" message={describeError(error).message} />
          : data.content.length === 0 ? <EmptyState title="No products here" message="Add your first product - it goes live after TriVoKo approves it." action={<Button to="/seller/products/new">New product</Button>} />
            : (
              <Card as="ul" className="divide-y divide-slate-200 dark:divide-slate-800">
                {data.content.map((p) => {
                  const s = STATUS[p.status] ?? { color: 'gray', label: p.status }
                  const stock = p.variants.reduce((n, v) => n + v.stock, 0)
                  return (
                    <li key={p.id}>
                      <Link to={`/seller/products/${p.id}`} className="flex flex-wrap items-center gap-3 px-4 py-3 hover:bg-slate-50 dark:hover:bg-slate-800/50">
                        {p.images[0]
                          ? <img src={p.images[0].url} alt="" className="h-12 w-12 rounded-lg object-cover" />
                          : <span className="flex h-12 w-12 items-center justify-center rounded-lg bg-slate-100 text-xs text-slate-400 dark:bg-slate-800">No photo</span>}
                        <span className="min-w-0 flex-1">
                          <span className="block truncate font-medium text-slate-900 dark:text-white">{p.name}</span>
                          <span className="text-xs text-slate-500 dark:text-slate-400">{p.categoryName} · {p.variants.length} variant(s) · {stock} in stock</span>
                        </span>
                        <span className="text-sm font-semibold">{formatRupees(p.priceFrom)}</span>
                        <Badge color={s.color}>{s.label}</Badge>
                      </Link>
                    </li>
                  )
                })}
              </Card>
            )}
    </div>
  )
}
