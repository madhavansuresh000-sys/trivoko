import { useSelector } from 'react-redux'
import { NavLink, Outlet } from 'react-router-dom'

import Button from '../../components/ui/Button'
import EmptyState from '../../components/ui/EmptyState'
import { StoreIcon } from '../../components/ui/icons'
import { isActiveSeller, selectUser } from '../../store/authSlice'

/**
 * The seller's back office (sketch 6): a side menu on laptops, a scrolling tab row on phones.
 * Only for an APPROVED shop; everyone else is pointed to "Become a seller". (The server checks again.)
 */
const LINKS = [
  { to: '/seller', label: 'Dashboard', end: true },
  { to: '/seller/orders', label: 'Orders to ship' },
  { to: '/seller/products', label: 'Products' },
  { to: '/seller/earnings', label: 'Earnings' },
  { to: '/seller/analytics', label: 'Analytics' },
  { to: '/seller/flash', label: 'Flash sale' },
]

const linkClass = ({ isActive }) =>
  'shrink-0 rounded-lg px-3 py-2 text-sm font-medium ' +
  (isActive ? 'bg-brand-50 text-brand-800 dark:bg-slate-800 dark:text-white' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800')

export default function SellerLayout() {
  const user = useSelector(selectUser)
  if (!isActiveSeller(user)) {
    return (
      <EmptyState icon={StoreIcon} title="This is the seller back office"
        message={user?.seller ? `Your shop is ${user.seller.status.toLowerCase()} - it opens here once TriVoKo approves it.` : 'Open a shop on TriVoKo to sell here.'}
        action={<Button to="/sell">{user?.seller ? 'See my application' : 'Become a seller'}</Button>} />
    )
  }
  return (
    <div className="grid gap-6 md:grid-cols-[12rem_1fr]">
      <nav aria-label="Seller menu" className="-mx-4 flex gap-1 overflow-x-auto px-4 md:mx-0 md:flex-col md:px-0">
        <p className="hidden px-3 pb-2 text-xs font-semibold uppercase tracking-wide text-slate-500 md:block dark:text-slate-400">{user.seller.shopName}</p>
        {LINKS.map((l) => <NavLink key={l.to} to={l.to} end={l.end} className={linkClass}>{l.label}</NavLink>)}
      </nav>
      <div className="min-w-0"><Outlet /></div>
    </div>
  )
}
