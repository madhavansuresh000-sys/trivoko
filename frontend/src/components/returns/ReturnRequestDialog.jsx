import { useState } from 'react'

import { requestReturn } from '../../api/returns'
import { describeError } from '../../hooks/useAsync'
import { formatRupees } from '../../lib/money'
import { REASONS } from '../../lib/returnReasons'
import Button from '../ui/Button'
import { SelectField, TextAreaField } from '../ui/FormField'
import Modal from '../ui/Modal'

/**
 * "Return this item" for one delivered order line. item = { id, productName, variantLabel, quantity, lineTotal,
 * discountShare }. The refund shown is what the server will pay back: line total minus its coupon share.
 */
export default function ReturnRequestDialog({ item, onClose, onDone }) {
  return (
    <Modal open={item !== null} onClose={onClose} title="Return this item">
      {item && <RequestForm item={item} onClose={onClose} onDone={onDone} />}
    </Modal>
  )
}

function RequestForm({ item, onClose, onDone }) {
  const [reason, setReason] = useState('')
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState({})
  const [busy, setBusy] = useState(false)
  const refund = Number(item.lineTotal) - Number(item.discountShare ?? 0)

  const submit = async (e) => {
    e.preventDefault()
    const found = {}
    if (!reason) found.reason = 'Please choose a reason'
    if (note.trim().length < 5 || note.trim().length > 300) found.note = 'Tell the seller what is wrong (5 to 300 characters)'
    setErrors(found)
    if (Object.keys(found).length) return
    setBusy(true)
    try {
      const ret = await requestReturn({ orderItemId: item.id, reason, note: note.trim() })
      onDone(ret)
    } catch (err) {
      setErrors({ form: describeError(err).message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <p className="text-sm text-slate-600 dark:text-slate-400">
        <strong className="text-slate-900 dark:text-white">{item.productName}</strong> · {item.variantLabel} × {item.quantity}
        <br />You get back <strong className="text-slate-900 dark:text-white">{formatRupees(refund)}</strong> once the courier collects it.
      </p>
      <SelectField id="return-reason" label="Why are you returning it?" placeholder="Choose a reason" options={REASONS}
        value={reason} error={errors.reason} onChange={(e) => setReason(e.target.value)} />
      <TextAreaField id="return-note" label="Tell the seller what is wrong" rows={3} value={note} error={errors.note}
        placeholder="e.g. The sole came apart after one day" onChange={(e) => setNote(e.target.value)} />
      {errors.form && <p role="alert" className="text-sm text-red-600 dark:text-red-400">{errors.form}</p>}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose}>Back</Button>
        <Button type="submit" disabled={busy}>{busy ? 'Sending…' : 'Ask for a return'}</Button>
      </div>
    </form>
  )
}
