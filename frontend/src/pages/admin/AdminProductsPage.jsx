import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { approveProduct, fetchAdminProduct, fetchAdminProducts, rejectProduct } from '../../api/admin'
import { PageTitle, SearchBox, StatusBadge, StatusTabs } from '../../components/admin/AdminBits'
import ReasonDialog from '../../components/admin/ReasonDialog'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatRupees } from '../../lib/money'
import { notify } from '../../store/notificationsSlice'

/** The product queue: open a product ("Check"), read its details and variants, then approve or reject with a reason. */
const TABS = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'ACTIVE', label: 'Live' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: '', label: 'All' },
]

export default function AdminProductsPage() {
  const dispatch = useDispatch()
  const [status, setStatus] = useState('PENDING')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [openId, setOpenId] = useState(null)
  const [rejecting, setRejecting] = useState(null)
  const { data, loading, error } = useAsync(() => fetchAdminProducts(status, q, page), [status, q, page, reload])

  const done = (message) => {
    dispatch(notify(message))
    setRejecting(null)
    setOpenId(null)
    setReload((r) => r + 1)
  }
  const approve = async (p) => {
    try {
      await approveProduct(p.id)
      done(`${p.name} is live`)
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  return (
    <div className="space-y-4">
      <PageTitle>Products</PageTitle>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <StatusTabs tabs={TABS} value={status} onChange={(s) => { setStatus(s); setPage(0) }} />
        <SearchBox label="products" placeholder="Product name or brand" onSearch={(text) => { setQ(text); setPage(0) }} />
      </div>

      {loading ? <Skeleton className="h-40" /> : error ? <Card className="p-4">{describeError(error).message}</Card>
        : data.content.length === 0 ? <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">No products here.</Card> : (
          <ul className="space-y-3">
            {data.content.map((p) => (
              <Card as="li" key={p.id} className="p-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex min-w-0 items-center gap-3">
                    {p.imageUrl
                      ? <img src={p.imageUrl} alt="" className="h-14 w-14 shrink-0 rounded-lg object-cover" />
                      : <div className="h-14 w-14 shrink-0 rounded-lg bg-slate-100 dark:bg-slate-800" aria-hidden="true" />}
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold text-slate-900 dark:text-white">{p.name}</span>
                        <StatusBadge status={p.status} />
                      </div>
                      <p className="text-sm text-slate-600 dark:text-slate-400">
                        {p.brand} · {p.categoryName} · {p.shopName} · from {formatRupees(p.priceFrom)} · {p.variantCount} variant(s)
                      </p>
                      {p.rejectionReason && <p className="text-sm text-red-700 dark:text-red-400">Rejected: {p.rejectionReason}</p>}
                    </div>
                  </div>
                  <div className="flex shrink-0 flex-wrap gap-2">
                    {p.status === 'ACTIVE' && <Button size="sm" variant="ghost" to={`/products/${p.slug}`}>View in shop</Button>}
                    <Button size="sm" variant="secondary" aria-expanded={openId === p.id}
                      onClick={() => setOpenId(openId === p.id ? null : p.id)}>{openId === p.id ? 'Close' : 'Check'}</Button>
                    {p.status === 'PENDING' && <>
                      <Button size="sm" onClick={() => approve(p)}>Approve</Button>
                      <Button size="sm" variant="ghost" onClick={() => setRejecting(p)}>Reject</Button>
                    </>}
                  </div>
                </div>
                {openId === p.id && <ProductDetails id={p.id} />}
              </Card>
            ))}
          </ul>
        )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}

      <ReasonDialog open={rejecting !== null} onClose={() => setRejecting(null)} title={rejecting ? `Reject ${rejecting.name}?` : ''}
        action="Reject" hint="The seller can fix it and send it again."
        onConfirm={async (reason) => {
          await rejectProduct(rejecting.id, reason)
          done(`${rejecting.name} rejected`)
        }} />
    </div>
  )
}

/** What the admin checks: the description, every variant with price / MRP / stock, and the photos. */
function ProductDetails({ id }) {
  const { data: p, loading, error } = useAsync(() => fetchAdminProduct(id), [id])
  if (loading) return <Skeleton className="mt-3 h-24" />
  if (error) return <p className="mt-3 text-sm">{describeError(error).message}</p>
  return (
    <div className="mt-4 space-y-3 border-t border-slate-200 pt-4 text-sm dark:border-slate-800">
      <p className="whitespace-pre-line text-slate-700 dark:text-slate-300">{p.description}</p>
      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead className="text-xs uppercase text-slate-500 dark:text-slate-400">
            <tr><th className="py-1 pr-3">Variant</th><th className="pr-3">SKU</th><th className="pr-3">Price</th><th className="pr-3">MRP</th><th>Stock</th></tr>
          </thead>
          <tbody>
            {p.variants.map((v) => (
              <tr key={v.id} className="border-t border-slate-100 dark:border-slate-800">
                <td className="py-1 pr-3">{v.label}</td><td className="pr-3 font-mono text-xs">{v.sku}</td>
                <td className="pr-3">{formatRupees(v.price)}</td><td className="pr-3">{formatRupees(v.mrp)}</td><td>{v.stock}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {p.images.length > 0 && (
        <div className="flex gap-2 overflow-x-auto">
          {p.images.map((img) => <img key={img.url} src={img.url} alt={img.altText} className="h-20 w-20 shrink-0 rounded-lg object-cover" />)}
        </div>
      )}
    </div>
  )
}
