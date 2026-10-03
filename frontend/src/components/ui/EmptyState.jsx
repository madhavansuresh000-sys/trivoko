import { InboxIcon } from './icons'

/** Friendly message when a list is empty, e.g. "No products match your filters". */
export default function EmptyState({ title, message, action, icon: Icon = InboxIcon }) {
  return (
    <div className="flex flex-col items-center rounded-2xl border border-dashed border-slate-300 px-6 py-12 text-center dark:border-slate-700">
      <Icon className="h-10 w-10 text-slate-400 dark:text-slate-500" />
      <h3 className="mt-4 text-lg font-semibold text-slate-900 dark:text-white">{title}</h3>
      {message && <p className="mt-1 max-w-sm text-sm text-slate-600 dark:text-slate-400">{message}</p>}
      {action && <div className="mt-6">{action}</div>}
    </div>
  )
}
