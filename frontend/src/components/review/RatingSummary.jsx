import { Stars } from '../ui/StarRating'

/** "4.3 ★★★★☆ 12 reviews" and one bar per star value, 5 at the top. */
export default function RatingSummary({ summary }) {
  if (!summary.count) {
    return <p className="text-sm text-slate-600 dark:text-slate-400">No reviews yet.</p>
  }
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:gap-8">
      <div className="text-center">
        <p className="text-4xl font-bold text-slate-900 dark:text-white">{Number(summary.average).toFixed(1)}</p>
        <Stars value={Number(summary.average)} />
        <p className="text-sm text-slate-600 dark:text-slate-400">{summary.count} {summary.count === 1 ? 'review' : 'reviews'}</p>
      </div>
      <ul className="w-full max-w-xs space-y-1" aria-label="Reviews per star">
        {[5, 4, 3, 2, 1].map((s) => {
          const n = summary.stars[s] ?? 0
          return (
            <li key={s} className="flex items-center gap-2 text-sm">
              <span className="w-6 text-slate-600 dark:text-slate-400">{s}★</span>
              <span className="h-2 flex-1 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                <span className="block h-full rounded-full bg-amber-500" style={{ width: `${(n / summary.count) * 100}%` }} />
              </span>
              <span className="w-6 text-right text-slate-600 dark:text-slate-400" aria-label={`${s} stars: ${n}`}>{n}</span>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
