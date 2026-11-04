import { fetchCards } from '../../api/catalog'
import useAsync from '../../hooks/useAsync'
import { recentlyViewed } from '../../lib/recentlyViewed'
import { ProductGrid } from './ProductCard'

/**
 * "Recently viewed" row (after launch), like a shop assistant who remembers what you looked at.
 * exceptId = the product on screen (it is not shown again). Hidden when there is nothing to show or on errors.
 */
export default function RecentlyViewed({ exceptId, limit = 4 }) {
  const ids = recentlyViewed.read().filter((id) => id !== exceptId).slice(0, limit)
  const key = ids.join(',')
  // Promise.resolve().then(...): any failure (even a missing answer) just hides the row
  const { data } = useAsync(() => (ids.length ? Promise.resolve().then(() => fetchCards(ids)).catch(() => []) : Promise.resolve([])), [key])
  if (!data || data.length === 0) return null
  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Recently viewed</h2>
      <ProductGrid products={data} />
    </section>
  )
}
