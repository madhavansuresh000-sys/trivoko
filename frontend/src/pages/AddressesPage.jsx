import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { createAddress, deleteAddress, fetchAddresses, makeDefaultAddress, updateAddress } from '../api/account'
import { Notice } from '../components/auth/AuthCard'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { SelectField, TextField } from '../components/ui/FormField'
import { MapPinIcon, PlusIcon } from '../components/ui/icons'
import { Spinner } from '../components/ui/Loader'
import Modal from '../components/ui/Modal'
import useAsync, { describeError } from '../hooks/useAsync'
import useForm from '../hooks/useForm'
import { notify } from '../store/notificationsSlice'
import { maxLength, required } from '../utils/validation'

/** My delivery addresses: at most 5, exactly one default (the server keeps both rules). */

const MAX = 5

const STATES = ['Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh', 'Delhi', 'Goa', 'Gujarat',
  'Haryana', 'Himachal Pradesh', 'Jammu and Kashmir', 'Jharkhand', 'Karnataka', 'Kerala', 'Ladakh', 'Madhya Pradesh',
  'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram', 'Nagaland', 'Odisha', 'Puducherry', 'Punjab', 'Rajasthan',
  'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
  'Andaman and Nicobar Islands', 'Chandigarh', 'Dadra and Nagar Haveli and Daman and Diu', 'Lakshadweep',
].map((s) => ({ value: s, label: s }))

const digits = (n, label) => (v) => (!v || new RegExp(`^\\d{${n}}$`).test(v.trim()) ? '' : `${label} must be ${n} digits`)

const rules = {
  name: [required('Name'), maxLength(100, 'Name')],
  phone: [required('Phone'), digits(10, 'Phone')],
  line1: [required('Address line 1'), maxLength(160, 'Address line 1')],
  line2: [maxLength(160, 'Address line 2')],
  city: [required('City'), maxLength(80, 'City')],
  state: [required('State')],
  pincode: [required('Pincode'), digits(6, 'Pincode')],
}

const EMPTY = { name: '', phone: '', line1: '', line2: '', city: '', state: 'Tamil Nadu', pincode: '' }

function AddressForm({ address, onSaved, onCancel }) {
  const [serverError, setServerError] = useState(null)
  const { field, handleSubmit, submitting } = useForm(
    address ? { ...EMPTY, ...address, line2: address.line2 ?? '' } : EMPTY,
    rules,
    async (v, { setServerErrors }) => {
      setServerError(null)
      const body = Object.fromEntries(Object.keys(EMPTY).map((k) => [k, v[k].trim()]))
      try {
        onSaved(address ? await updateAddress(address.id, body) : await createAddress(body))
      } catch (e) {
        const error = describeError(e)
        setServerError(error.message)
        setServerErrors(error.fieldErrors)
      }
    },
  )
  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-3">
      {serverError && <Notice tone="error">{serverError}</Notice>}
      <div className="grid gap-3 sm:grid-cols-2">
        <TextField label="Full name" autoComplete="name" {...field('name')} />
        <TextField label="Mobile number" type="tel" inputMode="numeric" autoComplete="tel-national" placeholder="9876543210" {...field('phone')} />
      </div>
      <TextField label="House no., building, street" autoComplete="address-line1" {...field('line1')} />
      <TextField label="Area, landmark (optional)" autoComplete="address-line2" {...field('line2')} />
      <div className="grid gap-3 sm:grid-cols-3">
        <TextField label="City" autoComplete="address-level2" {...field('city')} />
        <SelectField label="State" placeholder="Choose…" options={STATES} {...field('state')} />
        <TextField label="Pincode" inputMode="numeric" autoComplete="postal-code" placeholder="641001" {...field('pincode')} />
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save address'}</Button>
      </div>
    </form>
  )
}

export default function AddressesPage() {
  const dispatch = useDispatch()
  const [reload, setReload] = useState(0)
  const { data, loading, error } = useAsync(() => fetchAddresses(), [reload])
  const [editing, setEditing] = useState(null) // null = closed, {} = new, address = edit
  const [deleting, setDeleting] = useState(null)
  const addresses = data ?? []
  const refresh = () => setReload((r) => r + 1)

  const run = async (call, message) => {
    try {
      await call()
      dispatch(notify(message))
      refresh()
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">My addresses</h1>
          <p className="text-sm text-slate-600 dark:text-slate-400">{addresses.length} of {MAX} saved · the default one is used at checkout</p>
        </div>
        <Button onClick={() => setEditing({})} disabled={loading || addresses.length >= MAX}
          title={addresses.length >= MAX ? `You can save at most ${MAX} addresses` : undefined}>
          <PlusIcon className="h-4 w-4" /> Add address
        </Button>
      </div>

      {loading ? <Spinner /> : error ? <Notice tone="error">{describeError(error).message}</Notice>
        : addresses.length === 0 ? (
          <EmptyState icon={MapPinIcon} title="No addresses yet" message="Add one now and checkout will be quicker."
            action={<Button onClick={() => setEditing({})}>Add your first address</Button>} />
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2">
            {addresses.map((a) => (
              <Card as="li" key={a.id} className="flex flex-col p-4">
                <div className="flex items-start justify-between gap-2">
                  <p className="font-semibold text-slate-900 dark:text-white">{a.name}</p>
                  {a.isDefault && <Badge color="brand">Default</Badge>}
                </div>
                <address className="mt-1 text-sm not-italic text-slate-600 dark:text-slate-400">
                  {a.line1}{a.line2 && <>, {a.line2}</>}<br />{a.city}, {a.state} {a.pincode}<br />Phone {a.phone}
                </address>
                <div className="mt-auto flex flex-wrap gap-2 pt-3">
                  <Button size="sm" variant="secondary" onClick={() => setEditing(a)}>Edit</Button>
                  {!a.isDefault && (
                    <Button size="sm" variant="ghost" onClick={() => run(() => makeDefaultAddress(a.id), 'Default address changed')}>
                      Make default
                    </Button>
                  )}
                  <Button size="sm" variant="ghost" className="text-red-700 dark:text-red-400" onClick={() => setDeleting(a)}>Delete</Button>
                </div>
              </Card>
            ))}
          </ul>
        )}

      <Modal open={editing !== null} onClose={() => setEditing(null)} title={editing?.id ? 'Edit address' : 'Add an address'}>
        {editing !== null && (
          <AddressForm address={editing.id ? editing : null} onCancel={() => setEditing(null)}
            onSaved={() => { setEditing(null); dispatch(notify('Address saved')); refresh() }} />
        )}
      </Modal>

      <Modal open={deleting !== null} onClose={() => setDeleting(null)} title="Delete this address?"
        footer={<>
          <Button variant="secondary" onClick={() => setDeleting(null)}>Keep it</Button>
          <Button variant="danger" onClick={() => { const a = deleting; setDeleting(null); run(() => deleteAddress(a.id), 'Address deleted') }}>Delete</Button>
        </>}>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          {deleting?.name}, {deleting?.line1}, {deleting?.city}.
          {deleting?.isDefault && ' Another address will become your default.'}
        </p>
      </Modal>
    </div>
  )
}
