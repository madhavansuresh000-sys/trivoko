import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { adminApproveReturn, adminCloseReturn, fetchAdminReturns, retryReturnRefund } from '../../api/returns'
import { PageTitle, StatusTabs } from '../../components/admin/AdminBits'
import ReasonDialog from '../../components/admin/ReasonDialog'
import { ReturnHeading, ReturnStatusBadge, ReturnTimeline } from '../../components/returns/ReturnBits'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import EmptyState from '../../components/ui/EmptyState'
import { Skeleton } from '../../components/ui/Loader'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { notify } from '../../store/notificationsSlice'

const TABS = [
  { value: 'ESCALATED', label: 'Disputes' },
  { value: 'REFUND_FAILED', label: 'Failed refunds' },
]

/**
 * The admin's returns desk (Phase 8): disputes (a customer asked us to review a seller's "no") get the final word -
 * approve (the return goes ahead) or close (with a reason); failed refunds can be retried safely (same
 * idempotency key, so the money never goes out twice).
 */
export default function AdminReturnsPage() {
  const dispatch = useDispatch()
  const [status, setStatus] = useState('ESCALATED')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const { data, loading, error } = useAsync(() => fetchAdminReturns(status, page), [status, page, reload])
  const [closing, setClosing] = useState(null)
  const again = () => setReload((r) => r + 1)

  const act = async (call, message) => {
    try {
      const r = await call()
      dispatch(r.status === 'REFUND_FAILED' ? notify('The refund failed again - try later.', 'error') : notify(message))
      again()
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  const close = async (reason) => {
    await adminCloseReturn(closing.id, reason) // an error stays in the dialog
    setClosing(null)
    dispatch(notify('Return closed - the customer sees the reason.'))
    again()
  }

  return (
    <div className="space-y-4">
      <PageTitle>Returns</PageTitle>
      <StatusTabs tabs={TABS} value={status} label="Return status" onChange={(v) => { setStatus(v); setPage(0) }} />
      {loading ? <div className="space-y-3"><Skeleton className="h-24" /><Skeleton className="h-24" /></div>
        : error ? <EmptyState title="Could not load returns" message={describeError(error).message} />
          : data.content.length === 0 ? (
            <EmptyState title="Nothing waiting" message={status === 'ESCALATED' ? 'No customer has asked for a review.' : 'Every refund went through.'} />
          ) : (
            <ul className="space-y-3">
              {data.content.map((r) => (
                <li key={r.id}>
                  <Card className="space-y-3 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <ReturnHeading ret={r} />
                      <ReturnStatusBadge status={r.status} />
                    </div>
                    {r.rejectReason && <p className="text-sm text-slate-700 dark:text-slate-300"><strong>Seller's reason:</strong> {r.rejectReason}</p>}
                    <ReturnTimeline events={r.events} />
                    <div className="flex flex-wrap justify-end gap-2">
                      {r.status === 'ESCALATED' && <>
                        <Button size="sm" variant="secondary" onClick={() => setClosing(r)}>Close</Button>
                        <Button size="sm" onClick={() => act(() => adminApproveReturn(r.id), 'Approved - the return goes ahead.')}>Approve return</Button>
                      </>}
                      {r.status === 'REFUND_FAILED' && (
                        <Button size="sm" onClick={() => act(() => retryReturnRefund(r.id), 'Refund sent.')}>Retry refund</Button>
                      )}
                    </div>
                  </Card>
                </li>
              ))}
            </ul>
          )}
      {data && data.totalPages > 1 && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
      <ReasonDialog open={closing !== null} onClose={() => setClosing(null)} audience="customer"
        title="Close this return for good?" action="Close return" onConfirm={close}
        hint="Our decision is final - the customer cannot ask again." />
    </div>
  )
}
