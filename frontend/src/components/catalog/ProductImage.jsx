import useCategories from '../../hooks/useCategories'
import { lookOf } from '../../lib/categoryLook'

/**
 * The product photo - or, while the sample products have no photos yet, a soft tile in the category's
 * colour with its emoji and the brand, so the shop still looks tidy.
 */
export default function ProductImage({ src, alt, categorySlug, brand, className = '', large = false }) {
  const tree = useCategories()
  if (src) {
    return <img src={src} alt={alt} loading="lazy" className={`aspect-square w-full rounded-xl object-cover ${className}`} />
  }
  const look = lookOf(categorySlug, tree)
  return (
    <div role="img" aria-label={alt}
      className={`flex aspect-square w-full flex-col items-center justify-center rounded-xl ${look.tile} ${className}`}>
      <span aria-hidden="true" className={large ? 'text-8xl' : 'text-5xl'}>{look.emoji}</span>
      {brand && (
        <span aria-hidden="true" className={`mt-2 font-semibold uppercase tracking-widest text-slate-500 dark:text-slate-400 ${large ? 'text-sm' : 'text-[10px]'}`}>
          {brand}
        </span>
      )}
    </div>
  )
}
