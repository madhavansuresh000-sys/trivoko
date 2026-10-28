import { useSelector } from 'react-redux'
import { NavLink, Outlet } from 'react-router-dom'

import Button from '../../components/ui/Button'
import EmptyState from '../../components/ui/EmptyState'
import { UserIcon } from '../../components/ui/icons'
import { isAdmin, selectUser } from '../../store/authSlice'

/**
 * The mall office (Phase 6): a side menu on laptops, a scrolling tab row on phones - like the seller back office.
 * Only for ADMIN; everyone else sees a polite "not for you". (The server checks every URL again.)
 */
const LINKS = [
  { to: '/admin', label: 'Overview', end: true },
  { to: '/admin/shops', label: 'Shops' },
  { to: '/admin/products', label: 'Products' },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/orders', label: 'Orders' },
  { to: '/admin/returns', label: 'Returns' },
  { to: '/admin/coupons', label: 'Coupons' },
  { to: '/admin/reports', label: 'Reports' },
  { to: '/admin/flash', label: 'Flash sales' },
  { to: '/admin/activity', label: 'Activity log' },
]

const linkClass = ({ isActive }) =>
  'shrink-0 rounded-lg px-3 py-2 text-sm font-medium ' +
  (isActive ? 'bg-brand-50 text-brand-800 dark:bg-slate-800 dark:text-white' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800')

export default function AdminLayout() {
  const user = useSelector(selectUser)
  if (!isAdmin(user)) {
    return (
      <EmptyState icon={UserIcon} title="This page is for the TriVoKo team"
        message="Only the marketplace admin can open the admin panel."
        action={<Button to="/">Back to the shop</Button>} />
    )
  }
  return (
    <div className="grid gap-6 md:grid-cols-[12rem_1fr]">
      <nav aria-label="Admin menu" className="-mx-4 flex gap-1 overflow-x-auto px-4 md:mx-0 md:flex-col md:px-0">
        <p className="hidden px-3 pb-2 text-xs font-semibold uppercase tracking-wide text-slate-500 md:block dark:text-slate-400">Admin panel</p>
        {LINKS.map((l) => <NavLink key={l.to} to={l.to} end={l.end} className={linkClass}>{l.label}</NavLink>)}
      </nav>
      <div className="min-w-0"><Outlet /></div>
    </div>
  )
}
