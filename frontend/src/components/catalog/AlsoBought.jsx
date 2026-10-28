import { fetchAlsoBought } from '../../api/search'
import useAsync from '../../hooks/useAsync'
import { ProductGrid } from './ProductCard'

/** Fewer than this = hidden: one lonely card says little, and we never pad the list with random products. */
const MIN_TO_SHOW = 2

/** "Customers also bought" under the product page (Phase 10). Real paid orders only; quietly hidden on errors. */
export default function AlsoBought({ slug }) {
  const { data } = useAsync(() => fetchAlsoBought(slug).catch(() => []), [slug])
  if (!data || data.length < MIN_TO_SHOW) return null
  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Customers also bought</h2>
      <ProductGrid products={data} />
    </section>
  )
}
