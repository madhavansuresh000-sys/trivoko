import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { Link } from 'react-router-dom'

import { fetchWishlist } from '../api/wishlist'
import PriceTag from '../components/catalog/PriceTag'
import ProductImage from '../components/catalog/ProductImage'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import HeartIcon from '../components/ui/HeartIcon'
import { Spinner } from '../components/ui/Loader'
import useAsync, { describeError } from '../hooks/useAsync'
import { formatRupees } from '../lib/money'
import { notify } from '../store/notificationsSlice'
import { toggleWishlist } from '../store/wishlistSlice'

/** /wishlist - my saved products, with how much cheaper each one is than when I saved it. */
export default function WishlistPage() {
  const dispatch = useDispatch()
  const [reload, setReload] = useState(0)
  const { data, loading, error } = useAsync(() => fetchWishlist(), [reload])

  const remove = async (product) => {
    try {
      await dispatch(toggleWishlist({ productId: product.id, saved: true })).unwrap()
      setReload((n) => n + 1)
    } catch (e) {
      dispatch(notify(e.message ?? 'Could not remove it.', 'error'))
    }
  }

  if (loading) return <Spinner />
  if (error) return <EmptyState title="Could not load your wishlist" message={describeError(error).message} />

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">My wishlist</h1>
        <p className="text-sm text-slate-600 dark:text-slate-400">We send you one message when a saved product gets cheaper.</p>
      </div>
      {data.length === 0 ? (
        <EmptyState icon={HeartIcon} title="Nothing saved yet" message="Tap the heart on any product to save it here."
          action={<Button to="/products">Browse products</Button>} />
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2">
          {data.map(({ product, available, priceWhenAdded, priceDrop }) => (
            <li key={product.id}>
              <Card className="flex gap-3 p-3">
                <Link to={`/products/${product.slug}`} className="w-24 shrink-0">
                  <ProductImage src={product.imageUrl} alt={product.name} categorySlug={product.categorySlug} brand={product.brand} />
                </Link>
                <div className="flex min-w-0 flex-1 flex-col items-start gap-1">
                  <Link to={`/products/${product.slug}`} className="line-clamp-2 text-sm font-semibold text-slate-900 hover:underline dark:text-white">
                    {product.name}
                  </Link>
                  {available ? (
                    <PriceTag price={product.priceFrom} mrp={product.mrpFrom} discountPercent={product.discountPercent} />
                  ) : (
                    <Badge>Not available</Badge>
                  )}
                  {available && Number(priceDrop) > 0 && (
                    <Badge color="green">{formatRupees(priceDrop)} lower than when you saved it ({formatRupees(priceWhenAdded)})</Badge>
                  )}
                  {available && !product.inStock && <Badge color="red">Sold out</Badge>}
                  <Button size="sm" variant="ghost" className="mt-auto" onClick={() => remove(product)}>Remove</Button>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
