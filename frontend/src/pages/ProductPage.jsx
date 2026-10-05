import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { Link, useParams } from 'react-router-dom'

import { fetchProduct } from '../api/catalog'
import PriceTag from '../components/catalog/PriceTag'
import ProductImage from '../components/catalog/ProductImage'
import VariantPicker from '../components/catalog/VariantPicker'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { CartIcon, ChevronRightIcon, StoreIcon, TruckIcon } from '../components/ui/icons'
import { Skeleton } from '../components/ui/Loader'
import useAsync, { describeError } from '../hooks/useAsync'
import { addToCart } from '../store/cartSlice'
import { notify } from '../store/notificationsSlice'
import NotFoundPage from './NotFoundPage'

/**
 * The product page (sketch 3): photos, brand + name, the chosen variant's price, colour/size buttons,
 * stock hint, quantity, "Add to cart" and the seller box. Everything comes from GET /api/products/{slug}.
 */

/** Footwear > Running shoes - built from the category and its parent. */
function Breadcrumb({ category }) {
  const trail = []
  for (let c = category; c; c = c.parent) trail.unshift(c)
  return (
    <nav aria-label="Breadcrumb" className="flex flex-wrap items-center gap-1 text-sm text-slate-500 dark:text-slate-400">
      <Link to="/products" className="hover:text-brand-700 dark:hover:text-brand-300">All products</Link>
      {trail.map((c) => (
        <span key={c.slug} className="flex items-center gap-1">
          <ChevronRightIcon className="h-3.5 w-3.5" />
          <Link to={`/products?category=${c.slug}`} className="hover:text-brand-700 dark:hover:text-brand-300">{c.name}</Link>
        </span>
      ))}
    </nav>
  )
}

function Gallery({ product }) {
  const [index, setIndex] = useState(0)
  const images = product.images
  if (images.length === 0) {
    return <ProductImage alt={product.name} categorySlug={product.category.slug} brand={product.brand} large />
  }
  const main = images[Math.min(index, images.length - 1)]
  return (
    <div className="space-y-3">
      <ProductImage src={main.url} alt={main.altText || product.name} />
      {images.length > 1 && (
        <div className="flex gap-2 overflow-x-auto">
          {images.map((img, i) => (
            <button key={img.url} type="button" onClick={() => setIndex(i)} aria-label={`Photo ${i + 1}`}
              aria-pressed={i === index}
              className={`w-16 shrink-0 rounded-lg border-2 ${i === index ? 'border-brand-600' : 'border-transparent'}`}>
              <img src={img.url} alt="" className="aspect-square w-full rounded-md object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function StockHint({ variant }) {
  if (!variant.inStock) return <Badge color="red">Sold out</Badge>
  if (variant.onlyLeft) return <Badge color="amber">Only {variant.onlyLeft} left</Badge>
  return <Badge color="green">In stock</Badge>
}

function BuyBox({ product }) {
  const dispatch = useDispatch()
  const firstInStock = product.variants.find((v) => v.inStock) ?? product.variants[0]
  const [variantId, setVariantId] = useState(firstInStock?.id)
  const [quantity, setQuantity] = useState(1)
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(false)
  const variant = product.variants.find((v) => v.id === variantId) ?? firstInStock
  const maxQuantity = Math.min(10, variant?.onlyLeft ?? 10)

  if (!variant) return <p className="text-sm text-slate-600 dark:text-slate-400">This product has no sizes to buy yet.</p>

  const choose = (id) => {
    setVariantId(id)
    setQuantity(1)
    setAdded(false)
  }

  const add = async () => {
    setAdding(true)
    try {
      await dispatch(addToCart({ variantId: variant.id, quantity })).unwrap()
      dispatch(notify(`Added to cart: ${product.name}${product.variants.length > 1 ? ` (${variant.label})` : ''}`))
      setAdded(true)
    } catch (e) {
      dispatch(notify(e.message ?? 'Could not add to cart.', 'error'))
    } finally {
      setAdding(false)
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <PriceTag price={variant.price} mrp={variant.mrp} discountPercent={variant.discountPercent} size="lg" />
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Inclusive of all taxes</p>
      </div>
      <VariantPicker variants={product.variants} selectedId={variant.id} onSelect={choose} />
      <div className="flex flex-wrap items-center gap-3">
        <StockHint variant={variant} />
        {variant.inStock && (
          <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
            Quantity
            <select value={quantity} onChange={(e) => setQuantity(Number(e.target.value))}
              className="rounded-lg border border-slate-300 bg-white py-1 pl-2 pr-7 text-sm dark:border-slate-700 dark:bg-slate-900">
              {Array.from({ length: maxQuantity }, (_, i) => <option key={i + 1} value={i + 1}>{i + 1}</option>)}
            </select>
          </label>
        )}
      </div>
      <div className="flex flex-col gap-3 sm:flex-row">
        <Button size="lg" className="flex-1" disabled={!variant.inStock || adding} onClick={add}>
          <CartIcon className="h-5 w-5" /> {adding ? 'Adding…' : variant.inStock ? 'Add to cart' : 'Sold out'}
        </Button>
        {added && <Button size="lg" variant="secondary" to="/cart">Go to cart</Button>}
      </div>
    </div>
  )
}

function SellerBox({ seller }) {
  return (
    <Card className="space-y-2 p-4 text-sm">
      <p className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
        <StoreIcon className="h-4 w-4 text-brand-700 dark:text-brand-300" />
        Sold by <Link to={`/shops/${seller.slug}`} className="font-semibold text-brand-700 hover:underline dark:text-brand-300">{seller.shopName}</Link>
        <span className="text-slate-500 dark:text-slate-400">· {seller.city}</span>
      </p>
      <p className="flex items-center gap-2 text-slate-600 dark:text-slate-400">
        <TruckIcon className="h-4 w-4" /> Free delivery when this shop's items reach ₹499, else ₹40
      </p>
    </Card>
  )
}

function ProductSkeleton() {
  return (
    <div className="grid gap-8 md:grid-cols-2" aria-busy="true">
      <Skeleton className="aspect-square w-full rounded-xl" />
      <div className="space-y-4">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-8 w-3/4" />
        <Skeleton className="h-10 w-1/3" />
        <Skeleton className="h-12 w-full" />
      </div>
    </div>
  )
}

export default function ProductPage() {
  const { slug } = useParams()
  const { data: product, loading, error } = useAsync(() => fetchProduct(slug), [slug])

  if (loading) return <ProductSkeleton />
  if (error) {
    const { status, message } = describeError(error)
    if (status === 404) return <NotFoundPage />
    return <EmptyState title="Could not load this product" message={message} action={<Button to="/products">Back to products</Button>} />
  }

  return (
    <div className="space-y-6">
      <Breadcrumb category={product.category} />
      <div className="grid gap-8 md:grid-cols-2">
        <Gallery product={product} />
        <div className="space-y-5">
          <div>
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{product.brand}</p>
            <h1 className="mt-1 text-2xl font-bold text-slate-900 sm:text-3xl dark:text-white">{product.name}</h1>
          </div>
          {/* key: a different product starts with its own first variant */}
          <BuyBox key={product.id} product={product} />
          <SellerBox seller={product.seller} />
        </div>
      </div>
      <section className="max-w-3xl">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white">About this product</h2>
        <p className="mt-2 whitespace-pre-line text-sm leading-relaxed text-slate-700 dark:text-slate-300">{product.description}</p>
      </section>
    </div>
  )
}
