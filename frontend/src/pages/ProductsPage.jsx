import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { fetchBrands, fetchProducts } from '../api/catalog'
import FilterPanel from '../components/catalog/FilterPanel'
import { ProductGrid } from '../components/catalog/ProductCard'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { CloseIcon, FilterIcon, SearchIcon } from '../components/ui/icons'
import Pagination from '../components/ui/Pagination'
import useAsync, { describeError } from '../hooks/useAsync'
import useCategories from '../hooks/useCategories'
import { formatRupees } from '../lib/money'
import { activeFilterCount, parseFilters, toApiParams, toSearchParams } from '../lib/productFilters'

/**
 * The product list (sketch 2). Everything the customer chooses is written to the URL; the page only
 * reads the URL (lib/productFilters), so Back, Forward, refresh and shared links all show the same list.
 */

const SORTS = [
  { value: 'newest|asc', label: 'Newest first' },
  { value: 'price|asc', label: 'Price: low to high' },
  { value: 'price|desc', label: 'Price: high to low' },
]

function findName(tree, slug) {
  for (const top of tree) {
    if (top.slug === slug) return top.name
    const child = top.children.find((c) => c.slug === slug)
    if (child) return child.name
  }
  return null
}

/** A removable chip for each active filter: "Volta ×", "In stock ×", "₹100 – ₹2,000 ×". */
function ActiveChips({ filters, onChange }) {
  const chips = [
    ...(filters.q ? [{ key: 'q', label: `“${filters.q}”`, change: { q: '' } }] : []),
    ...filters.brands.map((b) => ({ key: `b-${b}`, label: b, change: { brands: filters.brands.filter((x) => x !== b) } })),
    ...(filters.minPrice !== null || filters.maxPrice !== null
      ? [{ key: 'price', label: `${filters.minPrice !== null ? formatRupees(filters.minPrice) : '₹0'} – ${filters.maxPrice !== null ? formatRupees(filters.maxPrice) : 'any'}`, change: { minPrice: null, maxPrice: null } }]
      : []),
    ...(filters.inStock ? [{ key: 'stock', label: 'In stock', change: { inStock: false } }] : []),
  ]
  if (chips.length === 0) return null
  return (
    <div className="flex flex-wrap gap-2">
      {chips.map((c) => (
        <button key={c.key} type="button" onClick={() => onChange(c.change)} aria-label={`Remove filter ${c.label}`}
          className="flex items-center gap-1 rounded-full bg-brand-700 px-3 py-1 text-xs font-semibold text-white hover:bg-brand-800">
          {c.label} <CloseIcon className="h-3.5 w-3.5" />
        </button>
      ))}
    </div>
  )
}

export default function ProductsPage() {
  const [params, setParams] = useSearchParams()
  const filters = parseFilters(params)
  const tree = useCategories()
  const [showFilters, setShowFilters] = useState(false)
  const [retry, setRetry] = useState(0)

  const key = toApiParams(filters).toString()
  const { data, loading, error } = useAsync(() => fetchProducts(toApiParams(filters)), [key, retry])
  const { data: brands } = useAsync(() => fetchBrands(filters.category).catch(() => []), [filters.category])

  /** Any change goes to the URL; a new filter starts again at page 1. */
  const update = (change) => {
    setParams(toSearchParams({ ...filters, page: 1, ...change }))
    if (change.page) window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const categoryName = findName(tree, filters.category)
  const title = filters.q ? `Results for “${filters.q}”` : categoryName ?? (filters.category ? 'Products' : 'All products')
  const sortValue = `${filters.sort}|${filters.dir}`
  const count = activeFilterCount(filters)
  const status = describeError(error)

  const panel = (prefix) => (
    <FilterPanel filters={filters} tree={tree} brands={brands ?? []} onChange={update} idPrefix={prefix} />
  )

  return (
    <div className="grid gap-6 md:grid-cols-[15rem_1fr]">
      <aside className="hidden md:block">
        <Card className="sticky top-20 p-4">{panel('d-')}</Card>
      </aside>

      <div className="min-w-0 space-y-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">{title}</h1>
            <p className="text-sm text-slate-600 dark:text-slate-400" aria-live="polite">
              {loading ? 'Loading…' : data ? `${data.totalElements} ${data.totalElements === 1 ? 'product' : 'products'}` : ''}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="secondary" size="sm" className="md:hidden" aria-expanded={showFilters}
              onClick={() => setShowFilters((s) => !s)}>
              <FilterIcon className="h-4 w-4" /> Filters{count > 0 && ` (${count})`}
            </Button>
            <label htmlFor="sort" className="sr-only">Sort by</label>
            <select id="sort" value={sortValue}
              onChange={(e) => { const [sort, dir] = e.target.value.split('|'); update({ sort, dir }) }}
              className="rounded-lg border border-slate-300 bg-white py-1.5 pl-3 pr-8 text-sm dark:border-slate-700 dark:bg-slate-900">
              {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </div>
        </div>

        {showFilters && <Card className="p-4 md:hidden">{panel('m-')}</Card>}

        <ActiveChips filters={filters} onChange={update} />

        {error ? (
          status.status === 404 ? (
            <EmptyState title="This category does not exist" message="It may have been renamed. Try the full list."
              action={<Button to="/products">See all products</Button>} />
          ) : (
            <EmptyState title="Could not load the products" message={status.message}
              action={<Button onClick={() => setRetry((r) => r + 1)}>Try again</Button>} />
          )
        ) : !loading && data?.content.length === 0 ? (
          <EmptyState icon={SearchIcon} title="No products match"
            message="Try fewer filters or another word. Typo-tolerant search is coming soon."
            action={<Button to="/products">Clear filters</Button>} />
        ) : (
          <ProductGrid products={data?.content ?? []} loading={loading} skeletons={8} />
        )}

        {data && data.totalPages > 1 && (
          <Pagination page={filters.page - 1} totalPages={data.totalPages} onChange={(p) => update({ page: p + 1 })} />
        )}

        {filters.category && !loading && (
          <p className="pt-2 text-sm text-slate-500 dark:text-slate-400">
            Looking for something else? <Link to="/products" className="font-medium text-brand-700 hover:underline dark:text-brand-300">Browse all products</Link>
          </p>
        )}
      </div>
    </div>
  )
}
