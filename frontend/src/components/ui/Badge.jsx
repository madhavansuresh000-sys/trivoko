const colors = {
  gray: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
  brand: 'bg-brand-100 text-brand-800 dark:bg-brand-900/60 dark:text-brand-200',
  green: 'bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-300',
  amber: 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-300',
  red: 'bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-300',
  accent: 'bg-accent-100 text-accent-800 dark:bg-accent-900/50 dark:text-accent-200',
}

/** Small pill label: In stock, 40% off, DELIVERED, PENDING, Sold out ... */
export default function Badge({ color = 'gray', className = '', children }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${colors[color]} ${className}`}>
      {children}
    </span>
  )
}
