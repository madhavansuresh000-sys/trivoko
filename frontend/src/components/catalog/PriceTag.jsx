import { formatRupees } from '../../lib/money'

/**
 * "₹2,499  ₹3,999  37% off" - the price, the crossed-out MRP and the saving (only when there is one).
 * size: 'sm' on cards, 'lg' on the product page.
 */
export default function PriceTag({ price, mrp, discountPercent, size = 'sm', from = false }) {
  const off = discountPercent ?? (mrp && price && Number(mrp) > Number(price)
    ? Math.round(((mrp - price) * 100) / mrp) : 0)
  return (
    <p className="flex flex-wrap items-baseline gap-x-2">
      {from && <span className="text-xs text-slate-500 dark:text-slate-400">from</span>}
      <span className={`font-bold text-slate-900 dark:text-white ${size === 'lg' ? 'text-3xl' : 'text-base'}`}>
        {formatRupees(price)}
      </span>
      {off > 0 && (
        <>
          <span className={`text-slate-500 line-through dark:text-slate-400 ${size === 'lg' ? 'text-base' : 'text-xs'}`}>
            <span className="sr-only">MRP </span>{formatRupees(mrp)}
          </span>
          <span className={`font-semibold text-green-700 dark:text-green-400 ${size === 'lg' ? 'text-base' : 'text-xs'}`}>
            {off}% off
          </span>
        </>
      )}
    </p>
  )
}
