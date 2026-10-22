import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { approveReturn, fetchSellerReturns, markReturnPickedUp, rejectReturn } from '../../api/returns'
import { StatusTabs } from '../../components/admin/AdminBits'
import ReasonDialog from '../../components/admin/ReasonDialog'
import { ReturnHeading, ReturnStatusBadge, ReturnTimeline } from '../../components/returns/ReturnBits'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import EmptyState from '../../components/ui/EmptyState'
import { ArrowLeftIcon } from '../../components/ui/icons'
import { Skeleton } from '../../components/ui/Loader'
import Modal from '../../components/ui/Modal'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatRupees } from '../../lib/money'
import { notify } from '../../store/notificationsSlice'

const TABS = [
  { value: 'REQUESTED', label: 'To answer' },
  { value: 'APPROVED', label: 'To collect' },
  { value: 'REFUNDED', label: 'Refunded' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: '', label: 'All' },
]

/**
 * The seller's returns (Phase 8): approve or reject (with a reason the customer reads), then "Picked up" when the
 * courier has the item - that refunds the customer at once and puts the item back in stock. A return left
 * unanswered for 2 days is approved automatically.
 */
export default function SellerReturnsPage() {
  const dispatch = useDispatch()
  const [status, setStatus] = useState('REQUESTED')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const { data, loading, error } = useAsync(() => fetchSellerReturns(status, page), [status, page, reload])
  const [rejecting, setRejecting] = useState(null)
  const [collecting, setCollecting] = useState(null)
  const [busy, setBusy] = useState(false)
  const again = () => setReload((r) => r + 1)

  const approve = async (r) => {
    try {
      await approveReturn(r.id)
      dispatch(notify('Approved - mark it picked up when the courier collects it.'))
      again()
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  const reject = async (reason) => {
    await rejectReturn(rejecting.id, reason) // an error stays in the dialog
    setRejecting(null)
    dispatch(notify('Return rejected - the customer sees your reason.'))
    again()
  }

  const pickedUp = async () => {
    setBusy(true)
    try {
      const done = await markReturnPickedUp(collecting.id)
      dispatch(done.status === 'REFUNDED'
        ? notify(`Refunded ${formatRupees(done.refundAmount)} - the item is back in stock.`)
        : notify('Picked up. The refund did not go through yet - TriVoKo will retry it.', 'error'))
      again()
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    } finally {
      setBusy(false)
      setCollecting(null)
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Returns</h1>
      <StatusTabs tabs={TABS} value={status} label="Return status" onChange={(v) => { setStatus(v); setPage(0) }} />
      {loading ? <div className="space-y-3"><Skeleton className="h-24" /><Skeleton className="h-24" /></div>
        : error ? <EmptyState title="Could not load returns" message={describeError(error).message} />
          : data.content.length === 0 ? (
            <EmptyState icon={ArrowLeftIcon} title="Nothing here" message="Returns your customers ask for show up here." />
          ) : (
            <ul className="space-y-3">
              {data.content.map((r) => (
                <li key={r.id}>
                  <Card className="space-y-3 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <ReturnHeading ret={r} showShop={false} />
                      <ReturnStatusBadge status={r.status} />
                    </div>
                    <p className="text-sm text-slate-700 dark:text-slate-300">“{r.note}”</p>
                    <details className="text-sm">
                      <summary className="cursor-pointer text-brand-700 dark:text-brand-300">Timeline</summary>
                      <div className="mt-2"><ReturnTimeline events={r.events} /></div>
                    </details>
                    <div className="flex flex-wrap justify-end gap-2">
                      {r.status === 'REQUESTED' && <>
                        <Button size="sm" variant="secondary" onClick={() => setRejecting(r)}>Reject</Button>
                        <Button size="sm" onClick={() => approve(r)}>Approve</Button>
                      </>}
                      {r.status === 'APPROVED' && <Button size="sm" onClick={() => setCollecting(r)}>Picked up</Button>}
                    </div>
                  </Card>
                </li>
              ))}
            </ul>
          )}
      {data && data.totalPages > 1 && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}

      <ReasonDialog open={rejecting !== null} onClose={() => setRejecting(null)} audience="customer"
        title={`Reject the return of ${rejecting?.productName ?? ''}?`} action="Reject return" onConfirm={reject}
        hint="The customer may ask TriVoKo to review it once." />
      <Modal open={collecting !== null} onClose={() => setCollecting(null)} title="Has the courier collected it?"
        footer={<>
          <Button variant="secondary" onClick={() => setCollecting(null)}>Not yet</Button>
          <Button disabled={busy} onClick={pickedUp}>{busy ? 'Refunding…' : 'Yes - refund the customer'}</Button>
        </>}>
        <p>{collecting ? formatRupees(collecting.refundAmount) : ''} goes back to the customer now, the item returns to your
          stock, and 90% of that amount comes off your earnings.</p>
      </Modal>
    </div>
  )
}
