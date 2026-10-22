import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import { fetchSalesReport, fetchTopProducts, fetchTopSellers } from '../../api/admin'
import { PageTitle, StatusTabs } from '../../components/admin/AdminBits'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatRupees } from '../../lib/money'

/**
 * The daily sales register: sales per day (bars), the top 10 products and shops, TriVoKo's 10% fee per shop.
 * A sale = a placed package that was not cancelled. Return rate comes in Phase 8 (no returns yet).
 */
const PERIODS = [{ value: 7, label: '7 days' }, { value: 30, label: '30 days' }, { value: 90, label: '90 days' }]
const shortDate = (iso) => new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })

export default function AdminReportsPage() {
  const [days, setDays] = useState(30)
  const sales = useAsync(() => fetchSalesReport(days), [days])
  const products = useAsync(() => fetchTopProducts(days), [days])
  const sellers = useAsync(() => fetchTopSellers(days), [days])
  const total = (sales.data ?? []).reduce((n, d) => n + Number(d.total), 0)
  const packages = (sales.data ?? []).reduce((n, d) => n + d.packages, 0)

  return (
    <div className="space-y-4">
      <PageTitle right={<StatusTabs tabs={PERIODS} value={days} onChange={setDays} label="Period" />}>Reports</PageTitle>

      <Card className="p-5">
        <p className="text-sm text-slate-600 dark:text-slate-400">
          Last {days} days: <strong className="text-slate-900 dark:text-white">{formatRupees(total)}</strong> from {packages} package(s)
          {' '}· TriVoKo fee (10%): <strong className="text-slate-900 dark:text-white">{formatRupees(Math.round(total * 10) / 100)}</strong>
        </p>
        <div className="mt-4 h-72" role="img" aria-label={`Bar chart of daily sales on TriVoKo for the last ${days} days`}>
          {sales.loading ? <Skeleton className="h-full" /> : sales.error ? <p>{describeError(sales.error).message}</p> : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={sales.data} margin={{ left: 8, right: 8 }}>
                <CartesianGrid stroke="var(--chart-grid)" vertical={false} />
                <XAxis dataKey="date" tickFormatter={shortDate} stroke="var(--chart-axis)" fontSize={12} minTickGap={16} />
                <YAxis stroke="var(--chart-axis)" fontSize={12} tickFormatter={(v) => `₹${v >= 1000 ? `${Math.round(v / 1000)}k` : v}`} />
                <Tooltip formatter={(v) => [formatRupees(v), 'Sales']} labelFormatter={shortDate}
                  contentStyle={{ background: 'var(--chart-surface)', border: '1px solid var(--chart-grid)', borderRadius: 8 }} />
                <Bar dataKey="total" fill="var(--chart-mark)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <TopTable title="Top products" state={products} empty="No sales in this period."
          head={['Product', 'Units', 'Sales']} row={(p) => [p.name, p.units, formatRupees(p.revenue)]} rowKey={(p) => p.slug} />
        <TopTable title="Top shops" state={sellers} empty="No sales in this period."
          head={['Shop', 'Packages', 'Sales', 'Fee']} row={(s) => [s.shopName, s.packages, formatRupees(s.revenue), formatRupees(s.fee)]}
          rowKey={(s) => s.sellerId} />
      </div>

      <Card className="p-4 text-sm text-slate-600 dark:text-slate-400">Return rate: available after Phase 8 (returns &amp; refunds).</Card>
    </div>
  )
}

function TopTable({ title, state, head, row, rowKey, empty }) {
  return (
    <Card className="p-4">
      <h2 className="mb-2 text-lg font-semibold text-slate-900 dark:text-white">{title}</h2>
      {state.loading ? <Skeleton className="h-32" /> : state.error ? <p className="text-sm">{describeError(state.error).message}</p>
        : state.data.length === 0 ? <p className="text-sm text-slate-600 dark:text-slate-400">{empty}</p> : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase text-slate-500 dark:text-slate-400">
                <tr>{head.map((h, i) => <th key={h} className={`py-1 ${i > 0 ? 'text-right' : ''}`}>{h}</th>)}</tr>
              </thead>
              <tbody>
                {state.data.map((d) => (
                  <tr key={rowKey(d)} className="border-t border-slate-100 dark:border-slate-800">
                    {row(d).map((cell, i) => <td key={i} className={`py-1.5 ${i > 0 ? 'text-right tabular-nums' : 'pr-2 text-slate-800 dark:text-slate-200'}`}>{cell}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
    </Card>
  )
}
