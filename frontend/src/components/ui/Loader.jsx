/** Spinning circle while something loads. */
export function Spinner({ label = 'Loading', className = 'h-8 w-8' }) {
  return (
    <div role="status" className="flex justify-center py-8">
      <span
        className={`${className} animate-spin rounded-full border-4 border-brand-200 border-t-brand-600 dark:border-slate-700 dark:border-t-brand-400`}
      />
      <span className="sr-only">{label}</span>
    </div>
  )
}

/** Grey placeholder block that "pulses" in the shape of the content that is coming. */
export function Skeleton({ className = '' }) {
  return <div className={`animate-pulse rounded-lg bg-slate-200 dark:bg-slate-800 ${className}`} />
}
