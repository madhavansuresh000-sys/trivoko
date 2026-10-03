/* Small inline icons (no icon library needed). All use currentColor. */

function Icon({ children, className = 'h-5 w-5', ...props }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round" className={className} aria-hidden="true" {...props}>
      {children}
    </svg>
  )
}

export const SunIcon = (p) => (
  <Icon {...p}><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></Icon>
)
export const MoonIcon = (p) => <Icon {...p}><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" /></Icon>
export const MenuIcon = (p) => <Icon {...p}><path d="M4 6h16M4 12h16M4 18h16" /></Icon>
export const BellIcon = (p) => (
  <Icon {...p}><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" /></Icon>
)
export const CloseIcon = (p) => <Icon {...p}><path d="M18 6 6 18M6 6l12 12" /></Icon>
export const SearchIcon = (p) => <Icon {...p}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></Icon>
export const CalendarIcon = (p) => (
  <Icon {...p}><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 11h18" /></Icon>
)
export const MapPinIcon = (p) => (
  <Icon {...p}><path d="M12 22s7-6.3 7-12a7 7 0 1 0-14 0c0 5.7 7 12 7 12z" /><circle cx="12" cy="10" r="2.5" /></Icon>
)
export const TicketIcon = (p) => (
  <Icon {...p}><path d="M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4z" /><path d="M14 6v12" strokeDasharray="2 2" /></Icon>
)
export const UsersIcon = (p) => (
  <Icon {...p}><circle cx="9" cy="8" r="3.5" /><path d="M2 20c0-3.3 3.1-6 7-6s7 2.7 7 6M16 4.5a3.5 3.5 0 0 1 0 7M22 20c0-2.6-1.9-4.8-4.5-5.6" /></Icon>
)
export const ArrowLeftIcon = (p) => <Icon {...p}><path d="M19 12H5M12 19l-7-7 7-7" /></Icon>
export const InboxIcon = (p) => (
  <Icon {...p}><path d="M22 12h-6l-2 3h-4l-2-3H2" /><path d="M5.5 5h13l3.5 7v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-6z" /></Icon>
)
export const CartIcon = (p) => (
  <Icon {...p}><circle cx="9" cy="20" r="1.5" /><circle cx="18" cy="20" r="1.5" /><path d="M2 3h3l2.7 12.4a2 2 0 0 0 2 1.6h8.6a2 2 0 0 0 2-1.6L22 7H6" /></Icon>
)
export const BoltIcon = (p) => <Icon {...p}><path d="M13 2 4 14h7l-1 8 9-12h-7z" /></Icon>
