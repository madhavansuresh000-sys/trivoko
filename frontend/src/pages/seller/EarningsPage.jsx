import { useState } from 'react'

import { fetchLedger } from '../../api/seller'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import Pagination from '../../components/ui/Pagination'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatWhen } from '../../lib/dates'
import { formatRupees } from '../../lib/money'

/**
 * My account book. The balance is the SUM of these lines: every delivered package adds 90% of what the customer
 * paid for it (TriVoKo keeps 10%). Returns (Phase 8) will add minus lines.
 */
export default function EarningsPage() {
  const [page, setPage] = useState(0)
  const { data, loading, error } = useAsync(() => fetchLedger(page), [page])

  if (loading) return <Skeleton className="h-48" />
  if (error) return <Card className="p-6">{describeError(error).message}</Card>

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Earnings</h1>
      <Card className="p-5">
        <p className="text-sm text-slate-600 dark:text-slate-400">Balance (what TriVoKo owes you)</p>
        <p className="mt-1 text-3xl font-extrabold text-slate-900 dark:text-white">{formatRupees(data.balance)}</p>
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">90% of every delivered package. TriVoKo keeps 10% as its fee.</p>
      </Card>
      {data.lines.content.length === 0 ? (
        <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">No earnings yet - they appear when a package is delivered.</Card>
      ) : (
        <Card className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-xs uppercase text-slate-500 dark:text-slate-400">
              <tr><th className="px-4 py-2">Date</th><th className="px-4 py-2">Order</th><th className="px-4 py-2">Details</th><th className="px-4 py-2 text-right">Amount</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {data.lines.content.map((l) => (
                <tr key={l.id}>
                  <td className="whitespace-nowrap px-4 py-2">{formatWhen(l.createdAt)}</td>
                  <td className="px-4 py-2 font-medium">{l.orderNumber}</td>
                  <td className="px-4 py-2 text-slate-600 dark:text-slate-400">{l.note}</td>
                  <td className={`px-4 py-2 text-right font-semibold ${l.amount < 0 ? 'text-red-700 dark:text-red-400' : 'text-green-700 dark:text-green-400'}`}>
                    {l.amount < 0 ? '−' : '+'}{formatRupees(Math.abs(l.amount))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      {data.lines.totalPages > 1 && <Pagination page={page} totalPages={data.lines.totalPages} onChange={setPage} />}
    </div>
  )
}
