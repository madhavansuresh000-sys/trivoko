import { Link } from 'react-router-dom'

import { fetchDashboard, fetchLowStock, fetchSellerPackages } from '../../api/seller'
import PackageCard from '../../components/seller/PackageCard'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import StatCard from '../../components/ui/StatCard'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatRupees } from '../../lib/money'
import { useState } from 'react'

/** Sketch 6: four numbers, the packages waiting for me, and what is running out. */
function greeting() {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'
}

export default function SellerDashboardPage() {
  const [reload, setReload] = useState(0)
  const stats = useAsync(() => fetchDashboard(), [reload])
  const toPack = useAsync(() => fetchSellerPackages('PLACED'), [reload])
  const toShip = useAsync(() => fetchSellerPackages('PACKED'), [reload])
  const low = useAsync(() => fetchLowStock(), [])

  if (stats.error) return <Card className="p-6">{describeError(stats.error).message}</Card>
  const s = stats.data
  const waiting = [...(toPack.data?.content ?? []), ...(toShip.data?.content ?? [])].slice(0, 6)

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900 dark:text-white">{greeting()}{s ? `, ${s.shopName}` : ''}</h1>
      {stats.loading ? <Skeleton className="h-24" /> : (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatCard label="To pack" value={s.toPack} hint={`${s.toShip} packed, waiting to ship`} />
          <StatCard label="Low stock" value={s.lowStock} hint="variants with fewer than 5" />
          <StatCard label="Balance" value={formatRupees(s.balance)} hint="90% of delivered packages" />
          <StatCard label="Today's sales" value={formatRupees(s.todaySales)} hint={`${s.waitingForApproval} product(s) waiting for approval`} />
        </div>
      )}

      <section className="space-y-3">
        <div className="flex items-end justify-between">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Packages to ship</h2>
          <Link to="/seller/orders" className="text-sm font-medium text-brand-700 hover:underline dark:text-brand-300">All orders ›</Link>
        </div>
        {toPack.loading || toShip.loading ? <Skeleton className="h-24" />
          : waiting.length === 0 ? <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">Nothing to pack or ship right now.</Card>
            : waiting.map((p) => <PackageCard key={p.id} pkg={p} onChanged={() => setReload((r) => r + 1)} />)}
      </section>

      {low.data?.length > 0 && (
        <section className="space-y-2">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Running low</h2>
          <Card as="ul" className="divide-y divide-slate-200 dark:divide-slate-800">
            {low.data.map((v) => (
              <li key={v.variantId} className="flex items-center justify-between gap-3 px-4 py-2 text-sm">
                <Link to={`/seller/products/${v.productId}`} className="text-slate-800 hover:underline dark:text-slate-200">{v.productName} · {v.variantLabel}</Link>
                <span className={`font-semibold ${v.stock === 0 ? 'text-red-700 dark:text-red-400' : 'text-amber-700 dark:text-amber-400'}`}>
                  {v.stock === 0 ? 'Sold out' : `${v.stock} left`}
                </span>
              </li>
            ))}
          </Card>
        </section>
      )}
    </div>
  )
}
