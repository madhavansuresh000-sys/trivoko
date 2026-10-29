import { useState } from 'react'
import { Link } from 'react-router-dom'

import { topCategoryOf } from '../../lib/categoryLook'
import Button from '../ui/Button'

/**
 * The listing's left column (sketch 2): category, brand, price, in stock.
 * It never keeps filters itself - every change calls onChange({...}), and the page writes it to the URL.
 */

function Heading({ children }) {
  return <h3 className="text-sm font-semibold text-slate-900 dark:text-white">{children}</h3>
}

const checkbox = 'h-4 w-4 rounded border-slate-300 text-brand-700 focus:ring-brand-600 dark:border-slate-600 dark:bg-slate-800'

function CategoryList({ tree, category }) {
  const top = topCategoryOf(category, tree)
  const link = (active) =>
    `block rounded px-1 py-0.5 text-sm ${active ? 'font-semibold text-brand-700 dark:text-brand-300' : 'text-slate-600 hover:text-brand-700 dark:text-slate-400 dark:hover:text-brand-300'}`
  if (!top) {
    return (
      <ul className="space-y-0.5">
        {tree.map((t) => (
          <li key={t.id}><Link to={`/products?category=${t.slug}`} className={link(false)}>{t.name}</Link></li>
        ))}
      </ul>
    )
  }
  // inside a category: its sub-categories (changing category starts a fresh search)
  return (
    <ul className="space-y-0.5">
      <li><Link to="/products" className={link(false)}>‹ All categories</Link></li>
      <li><Link to={`/products?category=${top.slug}`} className={link(top.slug === category)}>{top.name}</Link></li>
      {top.children.map((c) => (
        <li key={c.id} className="pl-3">
          <Link to={`/products?category=${c.slug}`} className={link(c.slug === category)}>{c.name}</Link>
        </li>
      ))}
    </ul>
  )
}

/**
 * Phase 10, while searching: the categories THIS search found, with counts. A click narrows the search (the
 * words stay); the normal category links would start a new, word-less list.
 */
function FoundIn({ categories, category, onChange }) {
  const button = (active) =>
    `block w-full rounded px-1 py-0.5 text-left text-sm ${active ? 'font-semibold text-brand-700 dark:text-brand-300' : 'text-slate-600 hover:text-brand-700 dark:text-slate-400 dark:hover:text-brand-300'}`
  return (
    <ul className="space-y-0.5">
      {category && (
        <li><button type="button" className={button(false)} onClick={() => onChange({ category: '' })}>‹ All categories</button></li>
      )}
      {categories.map((c) => (
        <li key={c.slug}>
          <button type="button" className={button(c.slug === category)} aria-pressed={c.slug === category}
            onClick={() => onChange({ category: c.slug })}>
            {c.name} <span className="text-slate-500 dark:text-slate-400">({c.count})</span>
          </button>
        </li>
      ))}
    </ul>
  )
}

/** Phase 10, while searching: ready-made price bands with how many results each has. */
function PriceBands({ bands, filters, onChange }) {
  return (
    <ul className="space-y-1">
      {bands.map((b) => {
        const active = filters.minPrice === b.min && filters.maxPrice === b.max
        return (
          <li key={b.label}>
            <button type="button" disabled={b.count === 0 && !active} aria-pressed={active}
              onClick={() => onChange(active ? { minPrice: null, maxPrice: null } : { minPrice: b.min, maxPrice: b.max })}
              className={`text-sm disabled:opacity-40 ${active ? 'font-semibold text-brand-700 dark:text-brand-300' : 'text-slate-600 hover:text-brand-700 dark:text-slate-400 dark:hover:text-brand-300'}`}>
              {b.label} <span className="text-slate-500 dark:text-slate-400">({b.count})</span>
            </button>
          </li>
        )
      })}
    </ul>
  )
}

/** Min / max price: typed freely, sent when "Apply" is pressed (not on every key). */
function PriceRange({ minPrice, maxPrice, onChange }) {
  const [min, setMin] = useState(minPrice ?? '')
  const [max, setMax] = useState(maxPrice ?? '')
  const num = (v) => (v === '' || Number(v) < 0 || Number.isNaN(Number(v)) ? null : Number(v))
  const swapped = num(min) !== null && num(max) !== null && num(min) > num(max)
  return (
    <form className="space-y-2" onSubmit={(e) => {
      e.preventDefault()
      if (!swapped) onChange({ minPrice: num(min), maxPrice: num(max) })
    }}>
      <div className="flex items-center gap-2">
        <label className="sr-only" htmlFor="minPrice">Minimum price</label>
        <input id="minPrice" type="number" min="0" inputMode="numeric" placeholder="₹ Min" value={min}
          onChange={(e) => setMin(e.target.value)}
          className="w-full rounded-lg border border-slate-300 px-2 py-1.5 text-sm dark:border-slate-700 dark:bg-slate-900" />
        <span className="text-slate-500 dark:text-slate-400">–</span>
        <label className="sr-only" htmlFor="maxPrice">Maximum price</label>
        <input id="maxPrice" type="number" min="0" inputMode="numeric" placeholder="₹ Max" value={max}
          onChange={(e) => setMax(e.target.value)}
          className="w-full rounded-lg border border-slate-300 px-2 py-1.5 text-sm dark:border-slate-700 dark:bg-slate-900" />
      </div>
      {swapped && <p className="text-xs text-red-700 dark:text-red-400">Min must not be more than max.</p>}
      <Button type="submit" variant="secondary" size="sm" className="w-full" disabled={swapped}>Apply price</Button>
    </form>
  )
}

/** facets = the smart search's counts (Phase 10), or null while simply browsing. */
export default function FilterPanel({ filters, tree, brands, facets = null, onChange, idPrefix = '' }) {
  const toggleBrand = (brand) => onChange({
    brands: filters.brands.includes(brand) ? filters.brands.filter((b) => b !== brand) : [...filters.brands, brand],
  })
  return (
    <div className="space-y-6">
      <section className="space-y-2">
        <Heading>Category</Heading>
        {facets
          ? <FoundIn categories={facets.categories} category={filters.category} onChange={onChange} />
          : <CategoryList tree={tree} category={filters.category} />}
      </section>

      {brands.length > 0 && (
        <section className="space-y-2">
          <Heading>Brand</Heading>
          <ul className="max-h-60 space-y-1 overflow-y-auto pr-1">
            {brands.map(({ brand, count }) => (
              <li key={brand}>
                <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
                  <input type="checkbox" className={checkbox} checked={filters.brands.includes(brand)} onChange={() => toggleBrand(brand)} />
                  {brand} <span className="text-slate-500 dark:text-slate-400">({count})</span>
                </label>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="space-y-2">
        <Heading>Price</Heading>
        {facets && <PriceBands bands={facets.prices} filters={filters} onChange={onChange} />}
        {/* key: when the URL changes (e.g. "Clear filters") the boxes show the new values */}
        <PriceRange key={`${filters.minPrice}-${filters.maxPrice}`} minPrice={filters.minPrice} maxPrice={filters.maxPrice} onChange={onChange} />
      </section>

      <section>
        <label htmlFor={`${idPrefix}inStock`} className="flex items-center gap-2 text-sm font-medium text-slate-700 dark:text-slate-300">
          <input id={`${idPrefix}inStock`} type="checkbox" className={checkbox} checked={filters.inStock}
            onChange={(e) => onChange({ inStock: e.target.checked })} />
          In stock only
        </label>
      </section>
    </div>
  )
}
