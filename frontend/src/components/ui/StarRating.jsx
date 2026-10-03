const labels = ['Poor', 'Not great', 'Okay', 'Good', 'Excellent']

/** Read-only stars: ★★★★☆ (with the number for screen readers). */
export function Stars({ value, className = '' }) {
  const full = Math.round(value)
  return (
    <span className={`text-amber-500 ${className}`} aria-label={`${value} out of 5 stars`} title={`${value} / 5`}>
      {'★'.repeat(full)}<span className="text-slate-300 dark:text-slate-600">{'★'.repeat(5 - full)}</span>
    </span>
  )
}

/**
 * Pick 1-5 stars. Built from radio buttons (hidden) + labels, so the keyboard (arrow keys)
 * and screen readers work without extra code.
 */
export function StarInput({ value, onChange, name = 'rating' }) {
  return (
    <fieldset>
      <legend className="sr-only">Your rating</legend>
      <div className="flex items-center gap-1">
        {[1, 2, 3, 4, 5].map((n) => (
          <label key={n} className="cursor-pointer" title={labels[n - 1]}>
            <input type="radio" name={name} value={n} checked={value === n} onChange={() => onChange(n)} className="peer sr-only" />
            <span className={`block rounded px-0.5 text-4xl leading-none transition-transform hover:scale-110 peer-focus-visible:ring-2 peer-focus-visible:ring-brand-500 ${
              n <= value ? 'text-amber-500' : 'text-slate-300 dark:text-slate-600'}`} aria-hidden="true">★</span>
            <span className="sr-only">{n} star{n > 1 ? 's' : ''} - {labels[n - 1]}</span>
          </label>
        ))}
        <span className="ml-2 text-sm font-medium text-slate-600 dark:text-slate-400">{value ? labels[value - 1] : 'Tap a star'}</span>
      </div>
    </fieldset>
  )
}
