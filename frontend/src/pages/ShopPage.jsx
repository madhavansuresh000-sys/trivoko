import { useState } from 'react'
import { useParams } from 'react-router-dom'

import { fetchProducts, fetchSeller } from '../api/catalog'
import { ProductGrid } from '../components/catalog/ProductCard'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { MapPinIcon, StoreIcon } from '../components/ui/icons'
import { Skeleton } from '../components/ui/Loader'
import Pagination from '../components/ui/Pagination'
import useAsync, { describeError } from '../hooks/useAsync'
import NotFoundPage from './NotFoundPage'

/** A seller's shop page: the shop header (GET /api/sellers/{slug}) and its products (?seller=slug). */
export default function ShopPage() {
  const { slug } = useParams()
  const [page, setPage] = useState(0)
  const shop = useAsync(() => fetchSeller(slug), [slug])
  const products = useAsync(() => fetchProducts({ seller: slug, page, size: 24 }), [slug, page])

  if (shop.error) {
    const { status, message } = describeError(shop.error)
    return status === 404 ? <NotFoundPage /> : <EmptyState title="Could not load this shop" message={message} />
  }

  const since = shop.data?.sellingSince
    ? new Date(shop.data.sellingSince).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' }) : null

  return (
    <div className="space-y-6">
      <Card className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center">
        <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-brand-100 text-brand-800 dark:bg-brand-900/60 dark:text-brand-200">
          <StoreIcon className="h-8 w-8" />
        </span>
        {shop.loading ? (
          <div className="flex-1 space-y-2"><Skeleton className="h-7 w-1/3" /><Skeleton className="h-4 w-1/2" /></div>
        ) : (
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">{shop.data.shopName}</h1>
            <p className="mt-1 flex flex-wrap items-center gap-x-3 text-sm text-slate-600 dark:text-slate-400">
              <span className="flex items-center gap-1"><MapPinIcon className="h-4 w-4" /> {shop.data.city}</span>
              {since && <span>Selling on TriVoKo since {since}</span>}
            </p>
            {shop.data.description && <p className="mt-2 max-w-2xl text-sm text-slate-700 dark:text-slate-300">{shop.data.description}</p>}
          </div>
        )}
      </Card>

      <section>
        <h2 className="mb-4 text-lg font-semibold text-slate-900 dark:text-white">
          Products{products.data ? ` (${products.data.totalElements})` : ''}
        </h2>
        {products.error ? (
          <EmptyState title="Could not load the products" message={describeError(products.error).message} />
        ) : !products.loading && products.data.content.length === 0 ? (
          <EmptyState title="No products yet" message="This shop has not listed anything yet." />
        ) : (
          <ProductGrid products={products.data?.content ?? []} loading={products.loading} />
        )}
        {products.data && products.data.totalPages > 1 && (
          <div className="mt-6"><Pagination page={page} totalPages={products.data.totalPages} onChange={setPage} /></div>
        )}
      </section>
    </div>
  )
}
