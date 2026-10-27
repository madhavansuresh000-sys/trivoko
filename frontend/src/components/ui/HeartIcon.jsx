/** A heart: outline when empty, filled when the product is on the wishlist. */
export default function HeartIcon({ filled = false, className = 'h-5 w-5' }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true" fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 21s-7.5-4.6-9.5-9.3C1.2 8.4 3.3 5 6.7 5c2 0 3.4 1.1 4.3 2.4h2C13.9 6.1 15.3 5 17.3 5c3.4 0 5.5 3.4 4.2 6.7C19.5 16.4 12 21 12 21z" />
    </svg>
  )
}
