import Button from './Button'

/** "Previous · Page 2 of 4 · Next". `page` starts at 0, like the API. */
export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null

  return (
    <nav className="mt-8 flex items-center justify-center gap-4" aria-label="Pages">
      <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
        ← Previous
      </Button>
      <span className="text-sm text-slate-600 dark:text-slate-400">
        Page <strong className="text-slate-900 dark:text-white">{page + 1}</strong> of {totalPages}
      </span>
      <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next →
      </Button>
    </nav>
  )
}
