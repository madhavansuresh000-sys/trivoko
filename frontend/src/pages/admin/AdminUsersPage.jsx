import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { blockUser, fetchUsers, unblockUser } from '../../api/admin'
import { PageTitle, SearchBox } from '../../components/admin/AdminBits'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import Modal from '../../components/ui/Modal'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { notify } from '../../store/notificationsSlice'

/**
 * Find a person and block / unblock them. Blocked = they cannot log in, and the login they have now stops working on
 * their next click. Admin accounts have no Block button (the server refuses it too).
 */
export default function AdminUsersPage() {
  const dispatch = useDispatch()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [confirming, setConfirming] = useState(null)
  const { data, loading, error } = useAsync(() => fetchUsers(q, page), [q, page, reload])

  const run = async (call, message) => {
    try {
      await call()
      dispatch(notify(message))
      setConfirming(null)
      setReload((r) => r + 1)
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  return (
    <div className="space-y-4">
      <PageTitle right={<SearchBox label="users" placeholder="Email, name or phone" onSearch={(text) => { setQ(text); setPage(0) }} />}>
        Users
      </PageTitle>

      {loading ? <Skeleton className="h-40" /> : error ? <Card className="p-4">{describeError(error).message}</Card>
        : data.content.length === 0 ? <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">Nobody found.</Card> : (
          <Card as="ul" className="divide-y divide-slate-200 dark:divide-slate-800">
            {data.content.map((u) => (
              <li key={u.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium text-slate-900 dark:text-white">{u.fullName}</span>
                    {u.roles.filter((r) => r !== 'CUSTOMER').map((r) => <Badge key={r} color={r === 'ADMIN' ? 'brand' : 'accent'}>{r.toLowerCase()}</Badge>)}
                    {u.blocked && <Badge color="red">blocked</Badge>}
                  </div>
                  <p className="break-all text-sm text-slate-600 dark:text-slate-400">
                    {u.email}{u.phone ? ` · ${u.phone}` : ''} · joined {formatWhen(u.createdAt)}
                  </p>
                </div>
                {!u.roles.includes('ADMIN') && (u.blocked
                  ? <Button size="sm" variant="secondary" onClick={() => run(() => unblockUser(u.id), `${u.fullName} can log in again`)}>Unblock</Button>
                  : <Button size="sm" variant="ghost" onClick={() => setConfirming(u)}>Block</Button>)}
              </li>
            ))}
          </Card>
        )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}

      <Modal open={confirming !== null} onClose={() => setConfirming(null)} title={confirming ? `Block ${confirming.fullName}?` : ''}>
        <p className="text-sm text-slate-700 dark:text-slate-300">They are logged out at once and cannot log in until you unblock them.</p>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setConfirming(null)}>Back</Button>
          <Button variant="danger" onClick={() => run(() => blockUser(confirming.id), `${confirming.fullName} is blocked`)}>Block</Button>
        </div>
      </Modal>
    </div>
  )
}
