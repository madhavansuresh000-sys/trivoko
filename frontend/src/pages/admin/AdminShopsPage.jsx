import { useState } from 'react'
import { useDispatch } from 'react-redux'

import { approveShop, blockShop, fetchShops, rejectShop, unblockShop } from '../../api/admin'
import { PageTitle, SearchBox, StatusBadge, StatusTabs } from '../../components/admin/AdminBits'
import ReasonDialog from '../../components/admin/ReasonDialog'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { notify } from '../../store/notificationsSlice'

/**
 * Shop applications and shops: approve / reject (with reason) a PENDING one, block (with reason) or unblock.
 * Blocking hides every product of the shop at once (the server's shop queries show only APPROVED shops).
 */
const TABS = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'BLOCKED', label: 'Blocked' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: '', label: 'All' },
]

export default function AdminShopsPage() {
  const dispatch = useDispatch()
  const [status, setStatus] = useState('PENDING')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [dialog, setDialog] = useState(null) // { shop, kind: 'reject' | 'block' }
  const { data, loading, error } = useAsync(() => fetchShops(status, q, page), [status, q, page, reload])

  const done = (message) => {
    dispatch(notify(message))
    setDialog(null)
    setReload((r) => r + 1)
  }
  const quick = async (call, message) => {
    try {
      await call()
      done(message)
    } catch (e) {
      dispatch(notify(describeError(e).message, 'error'))
    }
  }

  return (
    <div className="space-y-4">
      <PageTitle>Shops</PageTitle>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <StatusTabs tabs={TABS} value={status} onChange={(s) => { setStatus(s); setPage(0) }} />
        <SearchBox label="shops" placeholder="Shop name, city or slug" onSearch={(text) => { setQ(text); setPage(0) }} />
      </div>

      {loading ? <Skeleton className="h-40" /> : error ? <Card className="p-4">{describeError(error).message}</Card>
        : data.content.length === 0 ? <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">No shops here.</Card> : (
          <ul className="space-y-3">
            {data.content.map((s) => (
              <Card as="li" key={s.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0 space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-slate-900 dark:text-white">{s.shopName}</span>
                    <StatusBadge status={s.status} />
                  </div>
                  <p className="text-sm text-slate-600 dark:text-slate-400">
                    {s.city}{s.gstin ? ` · GSTIN ${s.gstin}` : ''} · applied {formatWhen(s.createdAt)}
                  </p>
                  {s.description && <p className="text-sm text-slate-700 dark:text-slate-300">{s.description}</p>}
                  {s.rejectReason && <p className="text-sm text-red-700 dark:text-red-400">Rejected: {s.rejectReason}</p>}
                  {s.blockReason && <p className="text-sm text-red-700 dark:text-red-400">Blocked: {s.blockReason}</p>}
                </div>
                <div className="flex shrink-0 flex-wrap gap-2">
                  {s.status === 'PENDING' && <>
                    <Button size="sm" onClick={() => quick(() => approveShop(s.id), `${s.shopName} approved`)}>Approve</Button>
                    <Button size="sm" variant="secondary" onClick={() => setDialog({ shop: s, kind: 'reject' })}>Reject</Button>
                  </>}
                  {s.status === 'BLOCKED'
                    ? <Button size="sm" variant="secondary" onClick={() => quick(() => unblockShop(s.id), `${s.shopName} unblocked`)}>Unblock</Button>
                    : <Button size="sm" variant="ghost" onClick={() => setDialog({ shop: s, kind: 'block' })}>Block</Button>}
                </div>
              </Card>
            ))}
          </ul>
        )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}

      <ReasonDialog open={dialog !== null} onClose={() => setDialog(null)}
        title={dialog ? `${dialog.kind === 'block' ? 'Block' : 'Reject'} ${dialog.shop.shopName}?` : ''}
        action={dialog?.kind === 'block' ? 'Block shop' : 'Reject'}
        hint={dialog?.kind === 'block' ? 'All products of this shop disappear from TriVoKo at once.' : undefined}
        onConfirm={async (reason) => {
          const { shop, kind } = dialog
          await (kind === 'block' ? blockShop(shop.id, reason) : rejectShop(shop.id, reason))
          done(kind === 'block' ? `${shop.shopName} blocked` : `${shop.shopName} rejected`)
        }} />
    </div>
  )
}
