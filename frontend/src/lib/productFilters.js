/**
 * The product list's filters live in the URL, e.g.
 *   /products?category=phones&brand=Volta&brand=Arc&inStock=true&sort=price&page=2
 * so Back/Forward work and a link can be shared. The page reads ONLY the URL:
 *
 *   URL  --parseFilters-->  filters object  --toApiParams-->  GET /api/products?...&page=1
 *    ^                          |
 *    +------ toSearchParams ----+   (when the customer ticks a box)
 *
 * People count pages from 1, the API from 0. Bad values (an old or hand-edited link) fall back to the defaults.
 */

export const PAGE_SIZE = 24

export const DEFAULT_FILTERS = Object.freeze({
  q: '',
  category: '',
  brands: [],
  minPrice: null,
  maxPrice: null,
  inStock: false,
  sort: 'newest',
  dir: 'asc',
  page: 1,
})

const SORTS = ['newest', 'price']
const DIRS = ['asc', 'desc']

/** A price must be a number >= 0; anything else = no price filter. */
function price(value) {
  if (value === null || value.trim() === '') return null
  const n = Number(value)
  return Number.isFinite(n) && n >= 0 ? n : null
}

export function parseFilters(params) {
  const page = Number(params.get('page'))
  return {
    q: (params.get('q') ?? '').trim(),
    category: params.get('category') ?? '',
    brands: params.getAll('brand').filter(Boolean),
    minPrice: price(params.get('minPrice')),
    maxPrice: price(params.get('maxPrice')),
    inStock: params.get('inStock') === 'true',
    sort: SORTS.includes(params.get('sort')) ? params.get('sort') : DEFAULT_FILTERS.sort,
    dir: DIRS.includes(params.get('dir')) ? params.get('dir') : DEFAULT_FILTERS.dir,
    page: Number.isInteger(page) && page >= 1 ? page : 1,
  }
}

/** Filters -> URL query. Defaults are left out, so the link stays short. */
export function toSearchParams(filters) {
  const p = new URLSearchParams()
  if (filters.q) p.set('q', filters.q)
  if (filters.category) p.set('category', filters.category)
  filters.brands.forEach((b) => p.append('brand', b))
  if (filters.minPrice !== null) p.set('minPrice', String(filters.minPrice))
  if (filters.maxPrice !== null) p.set('maxPrice', String(filters.maxPrice))
  if (filters.inStock) p.set('inStock', 'true')
  if (filters.sort !== DEFAULT_FILTERS.sort) p.set('sort', filters.sort)
  if (filters.dir !== DEFAULT_FILTERS.dir) p.set('dir', filters.dir)
  if (filters.page > 1) p.set('page', String(filters.page))
  return p
}

/** Filters -> GET /api/products query (page counted from 0, fixed page size). */
export function toApiParams(filters) {
  const p = toSearchParams({ ...filters, page: 1 })
  p.set('page', String(filters.page - 1))
  p.set('size', String(PAGE_SIZE))
  return p
}

/** How many filters the customer chose (for the "Filters (3)" button on phones). */
export const activeFilterCount = (f) =>
  f.brands.length + (f.minPrice !== null) + (f.maxPrice !== null) + (f.inStock ? 1 : 0)
