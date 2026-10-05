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
        <span className="text-slate-400">–</span>
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

export default function FilterPanel({ filters, tree, brands, onChange, idPrefix = '' }) {
  const toggleBrand = (brand) => onChange({
    brands: filters.brands.includes(brand) ? filters.brands.filter((b) => b !== brand) : [...filters.brands, brand],
  })
  return (
    <div className="space-y-6">
      <section className="space-y-2">
        <Heading>Category</Heading>
        <CategoryList tree={tree} category={filters.category} />
      </section>

      {brands.length > 0 && (
        <section className="space-y-2">
          <Heading>Brand</Heading>
          <ul className="max-h-60 space-y-1 overflow-y-auto pr-1">
            {brands.map(({ brand, count }) => (
              <li key={brand}>
                <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
                  <input type="checkbox" className={checkbox} checked={filters.brands.includes(brand)} onChange={() => toggleBrand(brand)} />
                  {brand} <span className="text-slate-400">({count})</span>
                </label>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="space-y-2">
        <Heading>Price</Heading>
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
