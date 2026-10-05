import Card from '../ui/Card'

/** Centered card used by the Login and Register pages. */
export default function AuthCard({ title, subtitle, children, footer }) {
  return (
    <div className="mx-auto w-full max-w-md py-4">
      <Card className="p-6 sm:p-8">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{subtitle}</p>}
        <div className="mt-6">{children}</div>
      </Card>
      {footer && <p className="mt-6 text-center text-sm text-slate-600 dark:text-slate-400">{footer}</p>}
    </div>
  )
}

/** Blue info box (green with tone="success", red with tone="error"). */
export function Notice({ tone = 'info', children }) {
  const tones = {
    info: 'border-brand-200 bg-brand-50 text-brand-800 dark:border-brand-800 dark:bg-slate-900 dark:text-brand-200',
    success: 'border-green-200 bg-green-50 text-green-800 dark:border-green-900 dark:bg-green-950/40 dark:text-green-300',
    error: 'border-red-200 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300',
  }
  return <div role="status" className={`rounded-lg border px-4 py-3 text-sm ${tones[tone]}`}>{children}</div>
}
