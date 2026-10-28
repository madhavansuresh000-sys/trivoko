import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { Link, useParams } from 'react-router-dom'

import { cancelReturn, escalateReturn, fetchMyReturns } from '../api/returns'
import ReasonDialog from '../components/admin/ReasonDialog'
import { ReturnHeading, ReturnStatusBadge, ReturnTimeline } from '../components/returns/ReturnBits'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import { ArrowLeftIcon } from '../components/ui/icons'
import { Spinner } from '../components/ui/Loader'
import useAsync, { describeError } from '../hooks/useAsync'
import { notify } from '../store/notificationsSlice'

/**
 * "My returns" (/account/returns, and /account/returns/:id from the refund bell, which puts that return first).
 * Each return shows its timeline; the only buttons are the customer's own moves: cancel while the seller has not
 * answered, and ask TriVoKo to look again once after a "no".
 */
export default function MyReturnsPage() {
  const { id } = useParams()
  const dispatch = useDispatch()
  const [reload, setReload] = useState(0)
  const { data, loading, error } = useAsync(fetchMyReturns, [reload])
  const [escalating, setEscalating] = useState(null)

  const cancel = async (ret) => {
    try {
      await cancelReturn(ret.id)
      dispatch(notify('Return cancelled - you keep the item.'))
      setReload((r) => r + 1)
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  const escalate = async (note) => {
    await escalateReturn(escalating.id, note) // an error stays in the dialog
    setEscalating(null)
    dispatch(notify('Sent to TriVoKo - we will look at it and decide.'))
    setReload((r) => r + 1)
  }

  if (loading) return <Spinner label="Loading your returns" />
  if (error) return <EmptyState title="Could not load your returns" message={describeError(error).message} />

  const list = id ? [...data].sort((a, b) => (String(b.id) === id) - (String(a.id) === id)) : data
  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div>
        <Link to="/orders" className="text-sm text-brand-700 hover:underline dark:text-brand-300">‹ My orders</Link>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">My returns</h1>
      </div>
      {list.length === 0 ? (
        <EmptyState icon={ArrowLeftIcon} title="No returns yet"
          message="Open a delivered order to return an item within 7 days of delivery." action={<Button to="/orders">My orders</Button>} />
      ) : (
        <ul className="space-y-3">
          {list.map((r) => (
            <li key={r.id}>
              <Card className={`space-y-3 p-4 sm:p-5 ${String(r.id) === id ? 'ring-2 ring-brand-500' : ''}`}>
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <ReturnHeading ret={r} />
                  <ReturnStatusBadge status={r.status} />
                </div>
                {r.status === 'REJECTED' && r.rejectReason && (
                  <p className="rounded-lg bg-red-50 p-3 text-sm text-red-800 dark:bg-red-950 dark:text-red-200">
                    The seller said: {r.rejectReason}
                  </p>
                )}
                <ReturnTimeline events={r.events} customerView />
                <div className="flex flex-wrap justify-end gap-2">
                  {r.status === 'REQUESTED' && <Button size="sm" variant="secondary" onClick={() => cancel(r)}>Cancel return</Button>}
                  {r.status === 'REJECTED' && <Button size="sm" onClick={() => setEscalating(r)}>Ask TriVoKo to review</Button>}
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
      <ReasonDialog open={escalating !== null} onClose={() => setEscalating(null)} audience="TriVoKo team"
        title="Ask TriVoKo to review?" action="Send for review" onConfirm={escalate}
        hint="You can ask once, within 3 days of the seller's answer. Our decision is final." />
    </div>
  )
}
