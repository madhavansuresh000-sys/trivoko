import { Link } from 'react-router-dom'

import { fetchOverview } from '../../api/admin'
import { PageTitle } from '../../components/admin/AdminBits'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import StatCard from '../../components/ui/StatCard'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatRupees } from '../../lib/money'

/** The admin's first screen: what is waiting for a decision, and how today is going. */
export default function AdminOverviewPage() {
  const { data: o, loading, error } = useAsync(() => fetchOverview(), [])
  if (error) return <Card className="p-6">{describeError(error).message}</Card>

  return (
    <div className="space-y-6">
      <PageTitle>Overview</PageTitle>
      {loading ? <Skeleton className="h-48" /> : (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <StatCard label="Sales today" value={formatRupees(o.salesToday)} hint={`${o.packagesSoldToday} package(s) sold`} />
            <StatCard label="Orders today" value={o.ordersToday} hint="paid or waiting for payment" />
            <StatCard label="Shops" value={o.approvedShops} hint={`${o.blockedShops} blocked`} />
            <StatCard label="Accounts" value={o.users} hint="customers, sellers and admins" />
          </div>
          <section className="space-y-3">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Waiting for you</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <QueueCard to="/admin/shops" count={o.pendingShops} what="shop application" />
              <QueueCard to="/admin/products" count={o.pendingProducts} what="product" />
            </div>
          </section>
        </>
      )}
    </div>
  )
}

function QueueCard({ to, count, what }) {
  return (
    <Card as={Link} to={to} className="flex items-center justify-between p-4 hover:border-brand-500">
      <span className="text-sm text-slate-700 dark:text-slate-300">
        {count === 0 ? `No ${what}s to check` : `${count} ${what}${count === 1 ? '' : 's'} to check`}
      </span>
      <span className={`text-2xl font-bold ${count > 0 ? 'text-amber-700 dark:text-amber-400' : 'text-slate-400'}`}>{count}</span>
    </Card>
  )
}
