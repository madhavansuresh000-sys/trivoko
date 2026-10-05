import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { Link } from 'react-router-dom'

import { applyAsSeller, fetchMyApplication } from '../api/account'
import { Notice } from '../components/auth/AuthCard'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import { TextAreaField, TextField } from '../components/ui/FormField'
import { Spinner } from '../components/ui/Loader'
import useAsync, { describeError } from '../hooks/useAsync'
import useForm from '../hooks/useForm'
import { loadSession } from '../store/authSlice'
import { notify } from '../store/notificationsSlice'
import { maxLength, minLength, required } from '../utils/validation'

/**
 * "Become a seller": the application form, or the status of my application.
 *   apply -> PENDING --admin approves--> APPROVED (+ SELLER role)   or   --rejects--> REJECTED (+ reason, apply again)
 * The admin pages come in Phase 6, the seller dashboard in Phase 5.
 */

const gstin = (v) => (!v || /^[0-9A-Z]{15}$/.test(v.trim()) ? '' : 'GSTIN is 15 capital letters or digits')

const rules = {
  shopName: [required('Shop name'), minLength(3, 'Shop name'), maxLength(120, 'Shop name')],
  city: [required('City'), minLength(2, 'City'), maxLength(80, 'City')],
  description: [maxLength(500, 'Description')],
  gstin: [gstin],
}

const STATUS = {
  PENDING: { color: 'amber', title: 'Waiting for approval', text: 'The TriVoKo team checks every new shop. You will see the answer here.' },
  APPROVED: { color: 'green', title: 'Your shop is open', text: 'Customers can find your shop. The seller dashboard (add products, see orders) comes soon.' },
  REJECTED: { color: 'red', title: 'Not approved', text: 'Please fix the points below and apply again.' },
  BLOCKED: { color: 'red', title: 'Shop blocked', text: 'This shop was blocked by TriVoKo. Please contact support.' },
}

function ApplicationForm({ previous, onDone }) {
  const dispatch = useDispatch()
  const [serverError, setServerError] = useState(null)
  const { field, handleSubmit, submitting } = useForm(
    { shopName: previous?.shopName ?? '', city: previous?.city ?? '', description: previous?.description ?? '', gstin: previous?.gstin ?? '' },
    rules,
    async (v, { setServerErrors }) => {
      setServerError(null)
      try {
        const application = await applyAsSeller({
          shopName: v.shopName.trim(), city: v.city.trim(), description: v.description.trim(), gstin: v.gstin.trim(),
        })
        await dispatch(loadSession()) // "who am I" now includes the shop
        dispatch(notify('Application sent! We will review it soon.'))
        onDone(application)
      } catch (e) {
        const error = describeError(e)
        setServerError(error.message)
        setServerErrors(error.fieldErrors)
      }
    },
  )
  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4">
      {serverError && <Notice tone="error">{serverError}</Notice>}
      <TextField label="Shop name" placeholder="Kovai Sports" {...field('shopName')} />
      <TextField label="City" autoComplete="address-level2" placeholder="Coimbatore" {...field('city')} />
      <TextAreaField label="About your shop (optional)" rows={3} placeholder="What do you sell?" {...field('description')} />
      <TextField label="GSTIN (optional)" placeholder="33ABCDE1234F1Z5" hint="15 characters. You can add it later."
        {...field('gstin')} />
      <Button type="submit" size="lg" className="w-full" disabled={submitting}>
        {submitting ? 'Sending…' : previous ? 'Apply again' : 'Send application'}
      </Button>
    </form>
  )
}

function StatusCard({ application, onApplyAgain }) {
  const s = STATUS[application.status]
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white">{application.shopName}</h2>
        <Badge color={s.color}>{application.status}</Badge>
      </div>
      <p className="font-medium text-slate-900 dark:text-white">{s.title}</p>
      <p className="text-sm text-slate-600 dark:text-slate-400">{s.text}</p>
      {application.rejectReason && <Notice tone="error">Reason: {application.rejectReason}</Notice>}
      {application.status === 'APPROVED' && <Button to={`/shops/${application.slug}`} variant="secondary">See my shop page</Button>}
      {application.status === 'REJECTED' && <Button onClick={onApplyAgain}>Fix and apply again</Button>}
    </div>
  )
}

export default function BecomeSellerPage() {
  const { data, loading, error } = useAsync(() => fetchMyApplication(), [])
  const [application, setApplication] = useState(undefined) // undefined = use what the server said
  const [editing, setEditing] = useState(false)
  const current = application === undefined ? data : application

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Sell on TriVoKo</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Reach customers across India. You pack your own parcels; TriVoKo takes payment once and pays you 90% of each sale.
        </p>
      </div>
      <Card className="p-6">
        {loading ? <Spinner label="Loading your application" />
          : error ? <Notice tone="error">{describeError(error).message}</Notice>
            : current && !editing ? <StatusCard application={current} onApplyAgain={() => setEditing(true)} />
              : <ApplicationForm previous={current} onDone={(a) => { setApplication(a); setEditing(false) }} />}
      </Card>
      <p className="text-center text-sm text-slate-500 dark:text-slate-400">
        Just shopping? <Link to="/products" className="font-medium text-brand-700 hover:underline dark:text-brand-300">Back to the shop</Link>
      </p>
    </div>
  )
}
