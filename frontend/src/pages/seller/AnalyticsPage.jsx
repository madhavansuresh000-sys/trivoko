import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import { fetchSales } from '../../api/seller'
import Card from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Loader'
import useAsync, { describeError } from '../../hooks/useAsync'
import { formatRupees } from '../../lib/money'

/**
 * Sales per day for the last 7 / 30 / 90 days (a sale = a placed package, not cancelled, at its total).
 * Recharts draws SVG, so it reads the --chart-* colour variables from index.css (light and dark mode).
 */
const PERIODS = [7, 30, 90]

const shortDate = (iso) => new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })

export default function AnalyticsPage() {
  const [days, setDays] = useState(7)
  const { data, loading, error } = useAsync(() => fetchSales(days), [days])
  const total = (data ?? []).reduce((n, d) => n + Number(d.total), 0)
  const packages = (data ?? []).reduce((n, d) => n + d.packages, 0)

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Analytics</h1>
        <div className="flex gap-1" role="tablist" aria-label="Period">
          {PERIODS.map((d) => (
            <button key={d} type="button" role="tab" aria-selected={days === d} onClick={() => setDays(d)}
              className={`rounded-full px-3 py-1.5 text-sm font-medium ${days === d ? 'bg-brand-700 text-white' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'}`}>
              {d} days
            </button>
          ))}
        </div>
      </div>
      <Card className="p-5">
        <p className="text-sm text-slate-600 dark:text-slate-400">
          Last {days} days: <strong className="text-slate-900 dark:text-white">{formatRupees(total)}</strong> from {packages} package(s)
        </p>
        <div className="mt-4 h-72" role="img" aria-label={`Bar chart of daily sales for the last ${days} days`}>
          {loading ? <Skeleton className="h-full" /> : error ? <p>{describeError(error).message}</p> : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data} margin={{ left: 8, right: 8 }}>
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
    </div>
  )
}
