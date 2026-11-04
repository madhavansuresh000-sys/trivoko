import { Link, useSearchParams } from 'react-router-dom'

import { fetchProduct } from '../api/catalog'
import PriceTag from '../components/catalog/PriceTag'
import ProductImage from '../components/catalog/ProductImage'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import { Spinner } from '../components/ui/Loader'
import { Stars } from '../components/ui/StarRating'
import useAsync from '../hooks/useAsync'
import { compareList, slugsFromQuery } from '../lib/compareList'
import { formatRupees } from '../lib/money'

/**
 * Compare two products side by side (after launch) - like holding two phones in the shop, one in each hand.
 * /compare?p=volta-v12-5g,kaveri-k5 (the link can be shared); without ?p= the two picked on product pages.
 * Always fresh data from GET /api/products/{slug}; a product that is no longer on sale is simply left out.
 */

const unique = (values) => [...new Set(values.filter(Boolean))]

/** The rows of the table: a label and how to show one product's value. */
const ROWS = [
  ['Price', (p, cheaper) => (
    <div>
      <PriceTag price={p.priceFrom} mrp={p.mrpFrom} discountPercent={p.discountPercent} from={p.variants.length > 1} />
      {cheaper && <span className="mt-1 inline-block rounded bg-green-100 px-2 py-0.5 text-xs font-semibold text-green-800 dark:bg-green-900/40 dark:text-green-300">Cheaper</span>}
    </div>
  )],
  ['Lowest in 30 days', (p) => (p.lowestPrice30Days == null ? '-' : formatRupees(p.lowestPrice30Days))],
  ['Rating', (p) => (p.ratingCount > 0
    ? <span className="inline-flex items-center gap-1"><Stars value={Number(p.ratingAvg)} /> {Number(p.ratingAvg).toFixed(1)} ({p.ratingCount})</span>
    : 'No reviews yet')],
  ['Brand', (p) => p.brand],
  ['Category', (p) => p.category.name],
  ['Sold by', (p) => <Link to={`/shops/${p.seller.slug}`} className="text-brand-700 hover:underline dark:text-brand-300">{p.seller.shopName}</Link>],
  ['Colours', (p) => unique(p.variants.map((v) => v.colour)).join(', ') || '-'],
  ['Sizes', (p) => unique(p.variants.map((v) => v.size)).join(', ') || '-'],
  ['In stock', (p) => {
    const inStock = p.variants.filter((v) => v.inStock).length
    return inStock === 0 ? 'Sold out' : `${inStock} of ${p.variants.length} options`
  }],
  ['About', (p) => <span className="line-clamp-6">{p.description}</span>],
]

export default function ComparePage() {
  const [params] = useSearchParams()
  const fromUrl = slugsFromQuery(params.get('p'))
  const slugs = fromUrl.length > 0 ? fromUrl : compareList.read()
  const key = slugs.join(',')
  // one missing product (sold out for good, blocked shop) must not hide the other
  const { data: products, loading } = useAsync(
    () => Promise.all(slugs.map((s) => fetchProduct(s).catch(() => null))).then((list) => list.filter(Boolean)),
    [key])

  if (slugs.length === 0) {
    return <EmptyState title="Nothing to compare yet" message="Open two products and press 'Compare' on each." action={<Button to="/products">Browse products</Button>} />
  }
  if (loading || !products) return <div className="flex justify-center py-16"><Spinner label="Loading the products" /></div>
  if (products.length === 0) {
    return <EmptyState title="These products are not available" message="They may be sold out for good." action={<Button to="/products">Browse products</Button>} />
  }

  const cheapest = products.length === 2 && Number(products[0].priceFrom) !== Number(products[1].priceFrom)
    ? (Number(products[0].priceFrom) < Number(products[1].priceFrom) ? products[0].id : products[1].id)
    : null

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Compare products</h1>
      {products.length < 2 && (
        <p className="text-sm text-slate-600 dark:text-slate-400">Pick one more product: open it and press "Compare".</p>
      )}
      <div className="overflow-x-auto">
        <table className="w-full min-w-[20rem] table-fixed border-collapse text-sm">
          <caption className="sr-only">Two products side by side</caption>
          <thead>
            <tr>
              <th scope="col" className="w-24 sm:w-40"><span className="sr-only">Detail</span></th>
              {products.map((p) => (
                <th key={p.id} scope="col" className="p-2 text-left align-top font-normal">
                  <Link to={`/products/${p.slug}`} className="block space-y-2">
                    <ProductImage src={p.images[0]?.url} alt={p.name} categorySlug={p.category.slug} brand={p.brand} />
                    <span className="block font-semibold text-slate-900 hover:underline dark:text-white">{p.name}</span>
                  </Link>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {ROWS.map(([label, show]) => (
              <tr key={label} className="border-t border-slate-200 dark:border-slate-700">
                <th scope="row" className="p-2 text-left align-top font-medium text-slate-600 dark:text-slate-400">{label}</th>
                {products.map((p) => (
                  <td key={p.id} className="p-2 align-top text-slate-800 dark:text-slate-200">{show(p, p.id === cheapest)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
