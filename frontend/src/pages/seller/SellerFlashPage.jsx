import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { fetchMyOffers, fetchOpenSales, offerDeal, withdrawOffer } from '../../api/flash'
import { fetchMyProducts } from '../../api/seller'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import { SelectField, TextField } from '../../components/ui/FormField'
import { Skeleton } from '../../components/ui/Loader'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { formatRupees } from '../../lib/money'
import { notify } from '../../store/notificationsSlice'

/**
 * Flash sale for sellers: put one of your live variants in a sale that has not started. The quantity leaves your
 * normal stock at once; unsold units come back when the sale ends. You can withdraw a deal until the sale starts.
 */
export default function SellerFlashPage() {
  const dispatch = useDispatch()
  const [reload, setReload] = useState(0)
  const [form, setForm] = useState({ saleId: '', variantId: '', salePrice: '', quantity: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const sales = useAsync(() => fetchOpenSales(), [reload])
  const offers = useAsync(() => fetchMyOffers(), [reload])
  const products = useAsync(() => fetchMyProducts('ACTIVE', 0), [])

  const upcoming = (sales.data ?? []).filter((s) => s.state === 'UPCOMING')
  const variants = (products.data?.content ?? []).flatMap((p) => p.variants.map((v) => ({ ...v, name: p.name })))
  const chosen = variants.find((v) => String(v.id) === form.variantId)

  const set = (name) => ({ id: `flash-${name}`, value: form[name], onChange: (e) => setForm((f) => ({ ...f, [name]: e.target.value })) })

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    if (!form.saleId || !form.variantId || !(Number(form.salePrice) > 0) || !(Number(form.quantity) >= 1)) {
      setError('Choose the sale, the item, a price and a quantity.')
      return
    }
    setBusy(true)
    try {
      await offerDeal(form.saleId, { variantId: Number(form.variantId), salePrice: Number(form.salePrice), quantity: Number(form.quantity) })
      dispatch(notify('Deal added to the sale'))
      setForm({ saleId: form.saleId, variantId: '', salePrice: '', quantity: '' })
      setReload((r) => r + 1)
    } catch (err) {
      setError(describeError(err).message)
    } finally {
      setBusy(false)
    }
  }

  const withdraw = async (o) => {
    try {
      await withdrawOffer(o.itemId)
      dispatch(notify('Deal withdrawn - the units are back in your stock'))
      setReload((r) => r + 1)
    } catch (err) {
      dispatch(notify(describeError(err).message, 'error'))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Flash sale</h1>
      <Card className="p-4">
        <h2 className="font-semibold text-slate-900 dark:text-white">Add a deal</h2>
        {sales.loading ? <Skeleton className="mt-3 h-24" /> : upcoming.length === 0 ? (
          <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">No sale is open for new deals. The admin schedules sales; check back soon.</p>
        ) : (
          <form onSubmit={submit} noValidate className="mt-3 grid gap-3 sm:grid-cols-2">
            <SelectField label="Sale" placeholder="Choose a sale" {...set('saleId')}
              options={upcoming.map((s) => ({ value: s.id, label: `${s.title} (${formatWhen(s.startsAt)})` }))} />
            <SelectField label="Item" placeholder="Choose an item" {...set('variantId')}
              options={variants.map((v) => ({ value: v.id, label: `${v.name} - ${v.label} (${formatRupees(v.price)}, ${v.stock} in stock)` }))} />
            <TextField label={`Sale price (₹)${chosen ? `, normal ${formatRupees(chosen.price)}` : ''}`} type="number" min="1" {...set('salePrice')} />
            <TextField label={`Quantity${chosen ? `, up to ${chosen.stock}` : ''}`} type="number" min="1" {...set('quantity')} />
            {error && <p role="alert" className="text-sm text-red-700 dark:text-red-400 sm:col-span-2">{error}</p>}
            <div className="sm:col-span-2"><Button type="submit" disabled={busy}>Add deal</Button></div>
          </form>
        )}
      </Card>

      <Card className="p-4">
        <h2 className="font-semibold text-slate-900 dark:text-white">My deals</h2>
        {offers.loading ? <Skeleton className="mt-3 h-20" /> : (offers.data ?? []).length === 0 ? (
          <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">You have no deals yet.</p>
        ) : (
          <ul className="mt-2 divide-y divide-slate-200 dark:divide-slate-800">
            {offers.data.map((o) => (
              <li key={o.itemId} className="flex flex-wrap items-center justify-between gap-2 py-3">
                <div>
                  <p className="font-medium text-slate-900 dark:text-white">{o.productName} ({o.variantLabel}) at {formatRupees(o.salePrice)}</p>
                  <p className="text-sm text-slate-600 dark:text-slate-400">{o.saleTitle} · {formatWhen(o.startsAt)} · {o.claimed}/{o.quantity} claimed</p>
                </div>
                {o.canWithdraw && <Button size="sm" variant="ghost" onClick={() => withdraw(o)}>Withdraw</Button>}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
