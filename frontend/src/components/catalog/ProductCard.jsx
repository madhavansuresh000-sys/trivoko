import { Link } from 'react-router-dom'

import Badge from '../ui/Badge'
import { Skeleton } from '../ui/Loader'
import PriceTag from './PriceTag'
import ProductImage from './ProductImage'

/** One tile of the product grid (API: ProductCard). The whole card is one link to the product page. */
export default function ProductCard({ product }) {
  return (
    <Link to={`/products/${product.slug}`}
      className="group flex flex-col rounded-2xl border border-slate-200 bg-white p-3 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:bg-slate-900">
      <div className="relative">
        <ProductImage src={product.imageUrl} alt={product.name} categorySlug={product.categorySlug} brand={product.brand} />
        {!product.inStock && (
          <Badge color="red" className="absolute left-2 top-2">Sold out</Badge>
        )}
      </div>
      <h3 className="mt-3 line-clamp-2 text-sm font-semibold text-slate-900 group-hover:text-brand-700 dark:text-white dark:group-hover:text-brand-300">
        {product.name}
      </h3>
      <div className="mt-1">
        <PriceTag price={product.priceFrom} mrp={product.mrpFrom} discountPercent={product.discountPercent} />
      </div>
      <p className="mt-auto pt-1 text-xs text-slate-500 dark:text-slate-400">{product.sellerName}</p>
    </Link>
  )
}

/** The grey shape of a card while the list loads. */
export function ProductCardSkeleton() {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-3 dark:border-slate-800 dark:bg-slate-900" aria-hidden="true">
      <Skeleton className="aspect-square w-full rounded-xl" />
      <Skeleton className="mt-3 h-4 w-4/5" />
      <Skeleton className="mt-2 h-4 w-1/2" />
      <Skeleton className="mt-2 h-3 w-1/3" />
    </div>
  )
}

/** A grid of cards; `loading` shows `skeletons` grey cards instead. */
export function ProductGrid({ products, loading = false, skeletons = 8, className = '' }) {
  return (
    <div className={`grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-4 ${className}`}
      aria-busy={loading || undefined}>
      {loading
        ? Array.from({ length: skeletons }, (_, i) => <ProductCardSkeleton key={i} />)
        : products.map((p) => <ProductCard key={p.id} product={p} />)}
    </div>
  )
}
