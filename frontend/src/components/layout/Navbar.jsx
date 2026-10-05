import { useEffect, useRef, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, NavLink, useLocation, useNavigate, useSearchParams } from 'react-router-dom'

import useCategories from '../../hooks/useCategories'
import useTheme from '../../hooks/useTheme'
import { lookOf } from '../../lib/categoryLook'
import { firstName, logout, selectAuthStatus, selectUser } from '../../store/authSlice'
import { selectCartCount } from '../../store/cartSlice'
import { notify } from '../../store/notificationsSlice'
import {
  CartIcon, ChevronDownIcon, CloseIcon, GridIcon, LogoutIcon, MapPinIcon, MenuIcon, MoonIcon, PackageIcon, SearchIcon,
  StoreIcon, SunIcon, UserIcon,
} from '../ui/icons'
import Logo from './Logo'

/**
 * The top bar on every page:
 *   logo · search · Categories · Become a seller · theme · cart (count) · account (or Login)
 * Phones: search on its own row, and a drawer (menu button) with categories and account links.
 */

const iconButton = 'rounded-lg p-2 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
const menuItem = 'flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800'

function ThemeToggle() {
  const { theme, toggleTheme } = useTheme()
  const dark = theme === 'dark'
  return (
    <button type="button" onClick={toggleTheme} className={iconButton}
      aria-label={dark ? 'Switch to light mode' : 'Switch to dark mode'} title={dark ? 'Light mode' : 'Dark mode'}>
      {dark ? <SunIcon /> : <MoonIcon />}
    </button>
  )
}

/** Sends the words to /products?q=... (the listing page does the searching). */
function SearchBox({ className = '' }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const location = useLocation()
  // show the current search while on the listing; empty elsewhere
  const current = location.pathname === '/products' ? params.get('q') ?? '' : ''
  return (
    <form role="search" className={`relative ${className}`} key={current}
      onSubmit={(e) => {
        e.preventDefault()
        const q = new FormData(e.currentTarget).get('q').trim()
        navigate(q ? `/products?q=${encodeURIComponent(q)}` : '/products')
      }}>
      <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
      <input type="search" name="q" defaultValue={current} placeholder="Search for products, brands and more"
        aria-label="Search products" enterKeyHint="search"
        className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm placeholder:text-slate-500 focus:border-brand-600 focus:bg-white dark:border-slate-700 dark:bg-slate-900 dark:placeholder:text-slate-400" />
    </form>
  )
}

/** A button that opens a small panel; closes on Escape, on a click outside and on page change. */
function Dropdown({ label, button, children, align = 'right' }) {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)
  const location = useLocation()
  const [lastPath, setLastPath] = useState(location.pathname)
  if (location.pathname !== lastPath) {
    setLastPath(location.pathname)
    setOpen(false)
  }
  useEffect(() => {
    if (!open) return undefined
    const close = (e) => {
      if (e.type === 'keydown' ? e.key === 'Escape' : !ref.current?.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', close)
    return () => {
      document.removeEventListener('mousedown', close)
      document.removeEventListener('keydown', close)
    }
  }, [open])
  return (
    <div className="relative" ref={ref}>
      <button type="button" aria-label={label} aria-expanded={open} aria-haspopup="true" onClick={() => setOpen((o) => !o)}
        className="flex items-center gap-1 rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800">
        {button}
        <ChevronDownIcon className="h-4 w-4" />
      </button>
      {open && (
        <div className={`absolute top-full z-50 mt-1 rounded-xl border border-slate-200 bg-white p-2 shadow-lg dark:border-slate-700 dark:bg-slate-900 ${align === 'right' ? 'right-0' : 'left-0'}`}>
          {children(() => setOpen(false))}
        </div>
      )}
    </div>
  )
}

function CategoriesMenu() {
  const tree = useCategories()
  return (
    <Dropdown label="All categories" align="left" button={<><GridIcon className="h-4 w-4" /> Categories</>}>
      {() => (
        <div className="grid w-[34rem] grid-cols-2 gap-x-4 gap-y-3 p-2">
          {tree.map((top) => (
            <div key={top.id}>
              <Link to={`/products?category=${top.slug}`} className="text-sm font-semibold text-slate-900 hover:text-brand-700 dark:text-white dark:hover:text-brand-300">
                <span aria-hidden="true">{lookOf(top.slug).emoji} </span>{top.name}
              </Link>
              <ul className="mt-1 space-y-0.5">
                {top.children.map((c) => (
                  <li key={c.id}>
                    <Link to={`/products?category=${c.slug}`} className="text-sm text-slate-600 hover:text-brand-700 dark:text-slate-400 dark:hover:text-brand-300">
                      {c.name}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </Dropdown>
  )
}

function useLogout() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  return async () => {
    await dispatch(logout())
    dispatch(notify('You are logged out. See you soon!'))
    navigate('/')
  }
}

/** "Hi, Ravi ▾" -> My addresses · Become a seller / My shop · Log out */
function AccountMenu({ user }) {
  const doLogout = useLogout()
  const shopLabel = user.seller ? 'My shop application' : 'Become a seller'
  return (
    <Dropdown label="Account menu" button={<><UserIcon className="h-4 w-4" /> Hi, {firstName(user)}</>}>
      {(close) => (
        <div className="w-56">
          <p className="truncate px-3 pb-2 pt-1 text-xs text-slate-500 dark:text-slate-400">{user.email}</p>
          <Link to="/orders" className={menuItem} onClick={close}><PackageIcon className="h-4 w-4" /> My orders</Link>
          <Link to="/account/addresses" className={menuItem} onClick={close}><MapPinIcon className="h-4 w-4" /> My addresses</Link>
          <Link to="/sell" className={menuItem} onClick={close}><StoreIcon className="h-4 w-4" /> {shopLabel}</Link>
          <button type="button" className={menuItem} onClick={() => { close(); doLogout() }}>
            <LogoutIcon className="h-4 w-4" /> Log out
          </button>
        </div>
      )}
    </Dropdown>
  )
}

function CartLink() {
  const count = useSelector(selectCartCount)
  return (
    <Link to="/cart" className={`relative ${iconButton}`} aria-label={`Cart, ${count} ${count === 1 ? 'item' : 'items'}`}>
      <CartIcon />
      {count > 0 && (
        <span aria-hidden="true" className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-accent-500 px-1 text-xs font-bold text-slate-950">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  )
}

/** The phone drawer: categories as a list, then account links. */
function MobileMenu({ user }) {
  const tree = useCategories()
  const doLogout = useLogout()
  const link = 'block rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800'
  return (
    <div className="max-h-[70vh] overflow-y-auto border-t border-slate-200 px-4 py-3 md:hidden dark:border-slate-800">
      <p className="px-3 pb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Shop by category</p>
      <div className="grid grid-cols-2 gap-1">
        {tree.map((top) => (
          <Link key={top.id} to={`/products?category=${top.slug}`} className={link}>
            <span aria-hidden="true">{lookOf(top.slug).emoji} </span>{lookOf(top.slug).short}
          </Link>
        ))}
      </div>
      <div className="mt-3 border-t border-slate-200 pt-3 dark:border-slate-800">
        {user ? (
          <>
            <p className="px-3 pb-1 text-xs text-slate-500 dark:text-slate-400">Logged in as {user.email}</p>
            <Link to="/orders" className={link}>My orders</Link>
            <Link to="/account/addresses" className={link}>My addresses</Link>
            <Link to="/sell" className={link}>{user.seller ? 'My shop application' : 'Become a seller'}</Link>
            <button type="button" className={`${link} w-full text-left`} onClick={doLogout}>Log out</button>
          </>
        ) : (
          <>
            <Link to="/login" className={link}>Log in</Link>
            <Link to="/register" className={link}>Create an account</Link>
            <Link to="/sell" className={link}>Become a seller</Link>
          </>
        )}
      </div>
    </div>
  )
}

export default function Navbar() {
  const [open, setOpen] = useState(false)
  const location = useLocation()
  const [lastPath, setLastPath] = useState(location.pathname + location.search)
  const user = useSelector(selectUser)
  const status = useSelector(selectAuthStatus)

  // close the phone menu after moving to another page
  if (location.pathname + location.search !== lastPath) {
    setLastPath(location.pathname + location.search)
    setOpen(false)
  }

  return (
    <header className="sticky top-0 z-40 border-b print:hidden border-slate-200 bg-white/90 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90">
      <nav className="mx-auto flex h-16 max-w-6xl items-center gap-2 px-4 lg:gap-4">
        <Logo />
        <div className="hidden md:block"><CategoriesMenu /></div>
        <SearchBox className="hidden flex-1 md:block" />
        <div className="ml-auto flex items-center gap-1 md:ml-0">
          {!user?.seller && (
            <NavLink to="/sell" className="hidden rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 lg:block dark:text-slate-200 dark:hover:bg-slate-800">
              Become a seller
            </NavLink>
          )}
          <ThemeToggle />
          <CartLink />
          <div className="hidden md:block">
            {user ? <AccountMenu user={user} /> : status === 'ready' && (
              <Link to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`}
                className="rounded-lg px-3 py-2 text-sm font-semibold text-brand-700 hover:bg-brand-50 dark:text-brand-300 dark:hover:bg-slate-800">
                Log in
              </Link>
            )}
          </div>
          <button type="button" className={`${iconButton} md:hidden`} onClick={() => setOpen((o) => !o)}
            aria-label={open ? 'Close menu' : 'Open menu'} aria-expanded={open}>
            {open ? <CloseIcon /> : <MenuIcon />}
          </button>
        </div>
      </nav>
      {/* phones: the search box gets its own row under the logo */}
      <div className="px-4 pb-3 md:hidden">
        <SearchBox />
      </div>
      {open && <MobileMenu user={user} />}
    </header>
  )
}
