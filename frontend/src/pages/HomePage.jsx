import { Link } from 'react-router-dom'

import { fetchProducts } from '../api/catalog'
import { ProductGrid } from '../components/catalog/ProductCard'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import { BoltIcon, ChevronRightIcon } from '../components/ui/icons'
import useAsync, { describeError } from '../hooks/useAsync'
import useCategories from '../hooks/useCategories'
import { lookOf } from '../lib/categoryLook'

/**
 * Home (sketch 1): category chips, the flash-sale banner place (the real sale is Phase 7),
 * "Top deals" (biggest % off among the newest products), "New arrivals" and category tiles.
 * One API call feeds both product rows.
 */

function SectionTitle({ title, to, linkText = 'See all' }) {
  return (
    <div className="mb-4 flex items-end justify-between gap-4">
      <h2 className="text-xl font-bold text-slate-900 dark:text-white">{title}</h2>
      {to && (
        <Link to={to} className="flex items-center gap-1 text-sm font-semibold text-brand-700 hover:underline dark:text-brand-300">
          {linkText} <ChevronRightIcon className="h-4 w-4" />
        </Link>
      )}
    </div>
  )
}

function CategoryChips({ tree }) {
  return (
    <div className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none]">
      {tree.map((top) => {
        const look = lookOf(top.slug)
        return (
          <Link key={top.id} to={`/products?category=${top.slug}`}
            className="flex shrink-0 items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:border-brand-600 hover:text-brand-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:text-brand-300">
            <span aria-hidden="true">{look.emoji}</span> {look.short}
          </Link>
        )
      })}
    </div>
  )
}

function FlashSaleBanner() {
  return (
    <section className="relative overflow-hidden rounded-3xl bg-brand-800 px-6 py-10 text-white sm:px-10">
      <Badge color="accent"><BoltIcon className="mr-1 h-3.5 w-3.5" /> Flash sale</Badge>
      <h1 className="mt-4 max-w-xl text-3xl font-extrabold tracking-tight sm:text-4xl">
        One cart. Many sellers. <span className="text-accent-300">One payment.</span>
      </h1>
      <p className="mt-3 max-w-lg text-brand-100">
        Shop from 8 Indian sellers in one go. Flash sales with a live countdown are coming soon.
      </p>
      <div className="mt-6 flex flex-wrap gap-3">
        <Button variant="deal" size="lg" to="/products">Start shopping</Button>
        <Button variant="light" size="lg" to="/products?inStock=true&sort=price">Lowest prices first</Button>
      </div>
      <span aria-hidden="true" className="pointer-events-none absolute -bottom-6 right-4 hidden text-[9rem] leading-none opacity-20 sm:block">🛍️</span>
    </section>
  )
}

function CategoryTiles({ tree }) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {tree.map((top) => {
        const look = lookOf(top.slug)
        return (
          <Link key={top.id} to={`/products?category=${top.slug}`}
            className={`flex flex-col items-center rounded-2xl p-4 text-center transition hover:-translate-y-0.5 hover:shadow-md ${look.tile}`}>
            <span aria-hidden="true" className="text-4xl">{look.emoji}</span>
            <span className="mt-2 text-sm font-semibold text-slate-800 dark:text-slate-100">{top.name}</span>
          </Link>
        )
      })}
    </div>
  )
}

export default function HomePage() {
  const tree = useCategories()
  const { data, loading, error } = useAsync(() => fetchProducts({ size: 24 }), [])
  const products = data?.content ?? []
  const deals = [...products].sort((a, b) => b.discountPercent - a.discountPercent).slice(0, 4)
  const newest = products.slice(0, 8)

  return (
    <div className="space-y-10">
      <CategoryChips tree={tree} />
      <FlashSaleBanner />

      {error ? (
        <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800 dark:bg-red-950/40 dark:text-red-300">
          {describeError(error).message}
        </p>
      ) : (
        <>
          <section>
            <SectionTitle title="Top deals" to="/products" />
            <ProductGrid products={deals} loading={loading} skeletons={4} />
          </section>
          <section>
            <SectionTitle title="New arrivals" to="/products" />
            <ProductGrid products={newest} loading={loading} skeletons={8} />
          </section>
        </>
      )}

      <section>
        <SectionTitle title="Shop by category" />
        <CategoryTiles tree={tree} />
      </section>
    </div>
  )
}
