import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { createCoupon, endCoupon, fetchCoupons, updateCoupon } from '../../api/admin'
import { PageTitle } from '../../components/admin/AdminBits'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import { SelectField, TextField } from '../../components/ui/FormField'
import { Skeleton } from '../../components/ui/Loader'
import Modal from '../../components/ui/Modal'
import useAsync, { describeError } from '../../hooks/useAsync'
import { EMPTY_COUPON, toForm, toRequest } from '../../lib/couponForm'
import { formatWhen } from '../../lib/dates'
import { formatRupees } from '../../lib/money'
import { notify } from '../../store/notificationsSlice'

/**
 * Coupons: create, edit (amounts, minimum, end date - never the code or type) and end. An ended coupon stops working
 * at checkout at once but stays in the list, because past orders used it.
 */
const describeCoupon = (c) =>
  c.type === 'FLAT' ? `${formatRupees(c.value)} off` : `${Number(c.value)}% off${c.maxDiscount ? ` (up to ${formatRupees(c.maxDiscount)})` : ''}`

export default function AdminCouponsPage() {
  const dispatch = useDispatch()
  const [reload, setReload] = useState(0)
  const [editing, setEditing] = useState(null) // null = closed, {} = new, coupon = edit
  const { data, loading, error } = useAsync(() => fetchCoupons(), [reload])

  const end = async (c) => {
    try {
      await endCoupon(c.id)
      dispatch(notify(`${c.code} ended`))
      setReload((r) => r + 1)
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  return (
    <div className="space-y-4">
      <PageTitle right={<Button onClick={() => setEditing({})}>New coupon</Button>}>Coupons</PageTitle>
      {loading ? <Skeleton className="h-40" /> : error ? <Card className="p-4">{describeError(error).message}</Card> : (
        <Card as="ul" className="divide-y divide-slate-200 dark:divide-slate-800">
          {data.map((c) => (
            <li key={c.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-mono font-semibold text-slate-900 dark:text-white">{c.code}</span>
                  <Badge color={c.active ? 'green' : 'gray'}>{c.active ? 'active' : 'ended'}</Badge>
                  <span className="text-sm text-slate-700 dark:text-slate-300">{describeCoupon(c)}</span>
                </div>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  {c.description} · min {formatRupees(c.minOrder)} · until {formatWhen(c.validUntil)}
                </p>
              </div>
              {c.active && (
                <div className="flex gap-2">
                  <Button size="sm" variant="secondary" onClick={() => setEditing(c)}>Edit</Button>
                  <Button size="sm" variant="ghost" onClick={() => end(c)}>End now</Button>
                </div>
              )}
            </li>
          ))}
        </Card>
      )}
      <CouponForm coupon={editing} onClose={() => setEditing(null)}
        onSaved={(saved, isNew) => { dispatch(notify(`${saved.code} ${isNew ? 'created' : 'saved'}`)); setEditing(null); setReload((r) => r + 1) }} />
    </div>
  )
}

function CouponForm({ coupon, onClose, onSaved }) {
  const isNew = coupon !== null && coupon.id === undefined
  const [form, setForm] = useState(EMPTY_COUPON)
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState(null)
  const [busy, setBusy] = useState(false)
  const [loadedFor, setLoadedFor] = useState(null)

  // fill the form when the dialog opens for a (new) coupon
  if (coupon !== loadedFor) {
    setLoadedFor(coupon)
    setErrors({})
    setMessage(null)
    setForm(coupon && !isNew ? toForm(coupon) : EMPTY_COUPON)
  }

  const field = (name) => ({ id: `coupon-${name}`, value: form[name], error: errors[name],
    onChange: (e) => setForm((f) => ({ ...f, [name]: e.target.value })) })

  const submit = async (e) => {
    e.preventDefault()
    const problems = {}
    if (isNew && !/^[A-Za-z0-9]{3,30}$/.test(form.code.trim())) problems.code = '3 to 30 letters or digits'
    if (!form.description.trim()) problems.description = 'Say what the customer gets'
    if (!(Number(form.value) > 0)) problems.value = 'More than 0'
    if (!form.validUntil) problems.validUntil = 'Choose the last day'
    setErrors(problems)
    if (Object.keys(problems).length > 0) return
    setBusy(true)
    try {
      const saved = isNew ? await createCoupon(toRequest(form, false)) : await updateCoupon(coupon.id, toRequest(form, true))
      onSaved(saved, isNew)
    } catch (err) {
      const { message: why, fieldErrors } = describeError(err)
      setErrors(fieldErrors)
      setMessage(why)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open={coupon !== null} onClose={onClose} title={isNew ? 'New coupon' : `Edit ${coupon?.code ?? ''}`}>
      <form onSubmit={submit} noValidate className="space-y-3">
        {isNew && <TextField label="Code" placeholder="e.g. DIWALI20" {...field('code')} />}
        <TextField label="Description (customers see it)" placeholder="20% off up to ₹300" {...field('description')} />
        {isNew && <SelectField label="Type" options={[{ value: 'PERCENT', label: 'Percent off' }, { value: 'FLAT', label: 'Rupees off' }]}
          placeholder="Choose" {...field('type')} />}
        <div className="grid grid-cols-2 gap-3">
          <TextField label={form.type === 'FLAT' ? 'Rupees off' : 'Percent off'} type="number" min="1" {...field('value')} />
          {form.type === 'PERCENT' && <TextField label="At most (₹, optional)" type="number" min="1" {...field('maxDiscount')} />}
          <TextField label="Minimum order (₹)" type="number" min="0" {...field('minOrder')} />
          <TextField label="Last day" type="date" {...field('validUntil')} />
        </div>
        {message && <p role="alert" className="text-sm text-red-700 dark:text-red-400">{message}</p>}
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>Back</Button>
          <Button type="submit" disabled={busy}>{isNew ? 'Create coupon' : 'Save'}</Button>
        </div>
      </form>
    </Modal>
  )
}
