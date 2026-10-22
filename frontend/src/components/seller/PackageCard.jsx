import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { cancelPackage, deliverPackage, packPackage, shipPackage } from '../../api/seller'
import { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { formatRupees } from '../../lib/money'
import { notify } from '../../store/notificationsSlice'
import { PackageStatusBadge } from '../order/OrderBits'
import Button from '../ui/Button'
import Card from '../ui/Card'
import { TextField } from '../ui/FormField'
import Modal from '../ui/Modal'

/**
 * One package on the seller's packing table, with the NEXT move as a button (the server's state machine decides
 * what is allowed; the buttons only show the moves that make sense):
 *   PLACED -> "Mark packed" | PACKED -> "Ship" (asks for the AWB) | SHIPPED -> "Mark delivered"
 *   PLACED or PACKED -> "Cancel" (asks for a reason; the customer is refunded)
 */
const TRACKING = /^[A-Za-z0-9]{6,30}$/

export default function PackageCard({ pkg, onChanged }) {
  const dispatch = useDispatch()
  const [busy, setBusy] = useState(false)
  const [dialog, setDialog] = useState(null) // 'ship' | 'cancel' | null
  const [text, setText] = useState('')
  const [error, setError] = useState(null)

  const run = async (call, message) => {
    setBusy(true)
    try {
      const updated = await call()
      dispatch(notify(message))
      setDialog(null)
      setText('')
      onChanged(updated)
    } catch (e) {
      const { message: why } = describeError(e)
      if (dialog) setError(why)
      else dispatch(notify(why, 'error'))
    } finally {
      setBusy(false)
    }
  }

  const open = (which) => {
    setDialog(which)
    setText('')
    setError(null)
  }

  const submitDialog = (e) => {
    e.preventDefault()
    if (dialog === 'ship') {
      if (!TRACKING.test(text.trim())) return setError('Enter the courier tracking number: 6 to 30 letters or digits.')
      return run(() => shipPackage(pkg.id, text.trim()), `Package ${pkg.orderNumber} shipped`)
    }
    if (text.trim().length < 5) return setError('Please tell the customer why (at least 5 characters).')
    return run(() => cancelPackage(pkg.id, text.trim()), `Package ${pkg.orderNumber} cancelled - the customer is refunded`)
  }

  const canCancel = pkg.status === 'PLACED' || pkg.status === 'PACKED'

  return (
    <Card className="p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-semibold text-slate-900 dark:text-white">
            {pkg.orderNumber} <span className="text-sm font-normal text-slate-500 dark:text-slate-400">· {formatWhen(pkg.placedAt)}</span>
          </p>
          <ul className="mt-1 text-sm text-slate-700 dark:text-slate-300">
            {pkg.items.map((i) => <li key={i.productName + i.variantLabel}>{i.productName} ({i.variantLabel}) × {i.quantity}</li>)}
          </ul>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            To {pkg.shipName}, {pkg.shipCity} {pkg.shipPincode} · {formatRupees(pkg.total)}
            {pkg.trackingNumber && <> · AWB {pkg.trackingNumber}</>}
            {pkg.cancelReason && <> · Cancelled: {pkg.cancelReason}</>}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <PackageStatusBadge status={pkg.status} />
          {pkg.status === 'PLACED' && <Button size="sm" disabled={busy} onClick={() => run(() => packPackage(pkg.id), `Package ${pkg.orderNumber} packed`)}>Mark packed</Button>}
          {pkg.status === 'PACKED' && <Button size="sm" disabled={busy} onClick={() => open('ship')}>Ship</Button>}
          {pkg.status === 'SHIPPED' && <Button size="sm" variant="secondary" disabled={busy} onClick={() => run(() => deliverPackage(pkg.id), `Package ${pkg.orderNumber} delivered`)}>Mark delivered</Button>}
          {canCancel && <Button size="sm" variant="ghost" disabled={busy} onClick={() => open('cancel')}>Cancel</Button>}
        </div>
      </div>

      <Modal open={dialog !== null} onClose={() => setDialog(null)}
        title={dialog === 'ship' ? `Ship ${pkg.orderNumber}` : `Cancel ${pkg.orderNumber}?`}>
        <form onSubmit={submitDialog} noValidate className="space-y-4">
          {dialog === 'ship' ? (
            <TextField id={`awb-${pkg.id}`} label="Tracking number (AWB)" placeholder="e.g. AWB12345678" value={text}
              onChange={(e) => setText(e.target.value)} error={error} autoFocus />
          ) : (
            <TextField id={`reason-${pkg.id}`} label="Reason (the customer sees it)" placeholder="e.g. Size UK 8 is damaged"
              value={text} onChange={(e) => setText(e.target.value)} error={error} autoFocus
              hint={`The customer gets ${formatRupees(pkg.total)} back and the stock returns to your shelf.`} />
          )}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDialog(null)}>Back</Button>
            <Button type="submit" variant={dialog === 'cancel' ? 'danger' : 'primary'} disabled={busy}>
              {dialog === 'ship' ? 'Mark shipped' : 'Cancel package'}
            </Button>
          </div>
        </form>
      </Modal>
    </Card>
  )
}
