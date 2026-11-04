import { useState } from 'react'
import { Link } from 'react-router-dom'

import { compareLink, compareList } from '../../lib/compareList'

/** "Compare" on the product page (after launch): pick up to two products, then open them side by side. */
export default function CompareButton({ slug }) {
  const [picked, setPicked] = useState(() => compareList.read())
  const added = picked.includes(slug)
  return (
    <div className="flex flex-wrap items-center gap-3 text-sm">
      <label className="inline-flex cursor-pointer items-center gap-2 text-slate-700 dark:text-slate-300">
        <input type="checkbox" checked={added} onChange={() => setPicked(compareList.toggle(slug))}
          className="h-4 w-4 rounded border-slate-300 text-brand-700 focus:ring-brand-500" />
        Compare
      </label>
      {added && picked.length === 2 && (
        <Link to={compareLink(picked)} className="font-semibold text-brand-700 hover:underline dark:text-brand-300">
          Compare now (2) →
        </Link>
      )}
      {added && picked.length === 1 && (
        <span className="text-slate-500 dark:text-slate-400">Open one more product to compare</span>
      )}
    </div>
  )
}
