import { useState } from 'react'

import Badge from '../ui/Badge'
import { SearchIcon } from '../ui/icons'

/** Small pieces shared by the admin pages (Phase 6). */

/** Pill tabs: [{ value: 'PENDING', label: 'Pending' }, ...]; value '' = all. */
export function StatusTabs({ tabs, value, onChange, label = 'Status' }) {
  return (
    <div className="-mx-4 flex gap-1 overflow-x-auto px-4 md:mx-0 md:px-0" role="tablist" aria-label={label}>
      {tabs.map((t) => (
        <button key={t.value} type="button" role="tab" aria-selected={value === t.value} onClick={() => onChange(t.value)}
          className={`shrink-0 rounded-full px-3 py-1.5 text-sm font-medium ${value === t.value ? 'bg-brand-700 text-white' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'}`}>
          {t.label}
        </button>
      ))}
    </div>
  )
}

/** A search box that only searches when Enter is pressed (or the button clicked), not on every letter. */
export function SearchBox({ label, placeholder, initial = '', onSearch }) {
  const [text, setText] = useState(initial)
  return (
    <form role="search" className="flex w-full max-w-md gap-2" onSubmit={(e) => { e.preventDefault(); onSearch(text.trim()) }}>
      <label className="sr-only" htmlFor={`search-${label}`}>{label}</label>
      <input id={`search-${label}`} type="search" value={text} placeholder={placeholder} onChange={(e) => setText(e.target.value)}
        className="block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" />
      <button type="submit" aria-label="Search" className="rounded-lg bg-brand-700 px-3 text-white hover:bg-brand-800">
        <SearchIcon className="h-4 w-4" />
      </button>
    </form>
  )
}

const STATUS_COLORS = {
  PENDING: 'amber', APPROVED: 'green', ACTIVE: 'green', PAID: 'green', REJECTED: 'red', BLOCKED: 'red',
  DRAFT: 'gray', EXPIRED: 'gray', PENDING_PAYMENT: 'amber',
}

/** PENDING (amber), APPROVED / ACTIVE (green), REJECTED / BLOCKED (red) ... */
export function StatusBadge({ status }) {
  return <Badge color={STATUS_COLORS[status] ?? 'gray'}>{status.replace('_', ' ').toLowerCase()}</Badge>
}

export const PageTitle = ({ children, right }) => (
  <div className="flex flex-wrap items-end justify-between gap-2">
    <h1 className="text-2xl font-bold text-slate-900 dark:text-white">{children}</h1>
    {right}
  </div>
)
