import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { createSale, fetchAdminSales } from '../../api/flash'
import { PageTitle, StatusBadge } from '../../components/admin/AdminBits'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import { TextField } from '../../components/ui/FormField'
import { Skeleton } from '../../components/ui/Loader'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { notify } from '../../store/notificationsSlice'

/** Flash sales: schedule a window; sellers then add their deals (Seller > Flash sale). */
const STATE_STATUS = { UPCOMING: 'PENDING', LIVE: 'ACTIVE', ENDED: 'EXPIRED' }

export default function AdminFlashSalesPage() {
  const dispatch = useDispatch()
  const [reload, setReload] = useState(0)
  const [form, setForm] = useState({ title: '', startsAt: '', endsAt: '' })
  const [errors, setErrors] = useState({})
  const [busy, setBusy] = useState(false)
  const { data, loading, error } = useAsync(() => fetchAdminSales(), [reload])

  const field = (name) => ({ id: `sale-${name}`, value: form[name], error: errors[name],
    onChange: (e) => setForm((f) => ({ ...f, [name]: e.target.value })) })

  const submit = async (e) => {
    e.preventDefault()
    const problems = {}
    if (!form.title.trim()) problems.title = 'Give the sale a name'
    if (!form.startsAt) problems.startsAt = 'Choose the start'
    if (!form.endsAt) problems.endsAt = 'Choose the end'
    if (form.startsAt && form.endsAt && form.endsAt <= form.startsAt) problems.endsAt = 'Must be after the start'
    setErrors(problems)
    if (Object.keys(problems).length > 0) return
    setBusy(true)
    try {
      await createSale({ title: form.title.trim(), startsAt: form.startsAt, endsAt: form.endsAt })
      dispatch(notify('Sale scheduled'))
      setForm({ title: '', startsAt: '', endsAt: '' })
      setReload((r) => r + 1)
    } catch (err) {
      const { message, fieldErrors } = describeError(err)
      setErrors({ ...fieldErrors, title: fieldErrors.title ?? message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-4">
      <PageTitle>Flash sales</PageTitle>
      <Card className="p-4">
        <form onSubmit={submit} noValidate className="grid gap-3 sm:grid-cols-4 sm:items-end">
          <TextField label="Name" placeholder="Midnight Deals" {...field('title')} />
          <TextField label="Starts" type="datetime-local" {...field('startsAt')} />
          <TextField label="Ends" type="datetime-local" {...field('endsAt')} />
          <Button type="submit" disabled={busy}>Schedule sale</Button>
        </form>
      </Card>
      {loading ? <Skeleton className="h-32" /> : error ? <Card className="p-4">{describeError(error).message}</Card> : data.length === 0 ? (
        <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">No sales yet. Schedule the first one above.</Card>
      ) : (
        <Card as="ul" className="divide-y divide-slate-200 dark:divide-slate-800">
          {data.map((s) => (
            <li key={s.id} className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <span className="font-semibold text-slate-900 dark:text-white">{s.title}</span>{' '}
                <StatusBadge status={STATE_STATUS[s.state]} /> <span className="text-xs text-slate-500">{s.state.toLowerCase()}</span>
                <p className="text-sm text-slate-600 dark:text-slate-400">{formatWhen(s.startsAt)} to {formatWhen(s.endsAt)}</p>
              </div>
              <p className="text-sm text-slate-700 dark:text-slate-300">{s.deals} deals · {s.claimed}/{s.units} claimed</p>
            </li>
          ))}
        </Card>
      )}
    </div>
  )
}
