/**
 * A small drawing for the 404 page: an empty shop shelf with one tipped-over box and a price tag.
 * Pure SVG in the brand colours (teal shelf, saffron tag), so it needs no image file and follows dark mode.
 * aria-hidden: it is decoration; the page heading says what happened.
 */
export default function EmptyShelfArt({ className = '' }) {
  return (
    <svg viewBox="0 0 240 150" className={className} aria-hidden="true" focusable="false">
      {/* back wall */}
      <rect x="20" y="10" width="200" height="120" rx="10" className="fill-brand-50 dark:fill-slate-800" />
      {/* two shelves */}
      <rect x="32" y="62" width="176" height="7" rx="3" className="fill-brand-600 dark:fill-brand-500" />
      <rect x="32" y="112" width="176" height="7" rx="3" className="fill-brand-600 dark:fill-brand-500" />
      {/* dashed outlines where products used to stand */}
      <rect x="48" y="30" width="26" height="32" rx="3" fill="none" strokeWidth="2" strokeDasharray="4 4"
        className="stroke-brand-300 dark:stroke-slate-600" />
      <rect x="86" y="38" width="22" height="24" rx="3" fill="none" strokeWidth="2" strokeDasharray="4 4"
        className="stroke-brand-300 dark:stroke-slate-600" />
      <rect x="172" y="80" width="26" height="32" rx="3" fill="none" strokeWidth="2" strokeDasharray="4 4"
        className="stroke-brand-300 dark:stroke-slate-600" />
      {/* one tipped-over box */}
      <g transform="rotate(-18 70 100)">
        <rect x="56" y="88" width="30" height="22" rx="3" className="fill-brand-200 dark:fill-brand-800" />
        <path d="M56 96h30" strokeWidth="2" className="stroke-brand-400 dark:stroke-brand-600" />
      </g>
      {/* saffron price tag hanging from the top shelf */}
      <path d="M150 69v8" strokeWidth="2" className="stroke-slate-400" />
      <path d="M138 77h24l-4 22h-16z" className="fill-accent-500" />
      <text x="150" y="92" textAnchor="middle" fontSize="9" fontWeight="700" className="fill-slate-950">404</text>
    </svg>
  )
}
