import { useState } from 'react'

import { describeError } from '../../hooks/useAsync'
import Button from '../ui/Button'
import { TextField } from '../ui/FormField'
import Modal from '../ui/Modal'

/**
 * "Why?" pop-up for reject / block. The reason is shown to the seller, so it must be a real sentence (5-300
 * characters, the same rule as the server). onConfirm(reason) returns a promise; a server error stays in the box.
 *   <ReasonDialog open={...} title="Reject Erode Organics?" action="Reject" onConfirm={(r) => rejectShop(id, r)} onClose={...} />
 */
export default function ReasonDialog({ open, title, onClose, ...formProps }) {
  return (
    <Modal open={open} onClose={onClose} title={title}>
      {/* mounted only while open, so every opening starts with an empty box */}
      {open && <ReasonForm onClose={onClose} {...formProps} />}
    </Modal>
  )
}

function ReasonForm({ action, hint, onConfirm, onClose }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    const text = reason.trim()
    if (text.length < 5 || text.length > 300) return setError('Please write the reason (5 to 300 characters) - the seller sees it.')
    setBusy(true)
    try {
      await onConfirm(text)
    } catch (err) {
      setError(describeError(err).message)
    } finally {
      setBusy(false)
    }
    return undefined
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <TextField id="admin-reason" label="Reason (the seller sees it)" value={reason} error={error} hint={hint}
        placeholder="e.g. The photos show a different brand" onChange={(e) => setReason(e.target.value)} />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose}>Back</Button>
        <Button type="submit" variant="danger" disabled={busy}>{action}</Button>
      </div>
    </form>
  )
}
