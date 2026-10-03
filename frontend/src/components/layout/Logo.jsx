import { Link } from 'react-router-dom'

/** The TriVoKo mark (same drawing as public/favicon.svg). */
export function LogoMark({ className = 'h-8 w-8' }) {
  return (
    <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
      <rect width="32" height="32" rx="8" fill="#0f766e" />
      <path d="M12.5 12v-1.5a3.5 3.5 0 0 1 7 0V12" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" />
      <path d="M8.5 12h15l-1.3 11.3a2 2 0 0 1-2 1.7h-8.4a2 2 0 0 1-2-1.7z" fill="#fff" />
      <path d="M16 15.2l3.6 6.1h-7.2z" fill="#f97316" />
    </svg>
  )
}

/** Mark + name: Tri (ink) Vo (teal) Ko (saffron). */
export default function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2 text-xl font-extrabold tracking-tight text-slate-900 dark:text-white">
      <LogoMark />
      <span>
        Tri<span className="text-brand-700 dark:text-brand-400">Vo</span>
        <span className="text-accent-600 dark:text-accent-400">Ko</span>
      </span>
    </Link>
  )
}
