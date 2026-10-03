import Card from './Card'

/** One number with a label, for dashboards: <StatCard label="Orders today" value={12} hint="Across all your packages" /> */
export default function StatCard({ label, value, hint }) {
  return (
    <Card className="p-5">
      <p className="text-sm text-slate-500 dark:text-slate-400">{label}</p>
      <p className="mt-1 text-3xl font-extrabold text-slate-900 dark:text-white">{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{hint}</p>}
    </Card>
  )
}
