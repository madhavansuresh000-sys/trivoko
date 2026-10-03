import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'

import useTheme from '../../hooks/useTheme'
import { CartIcon, CloseIcon, MenuIcon, MoonIcon, SearchIcon, SunIcon } from '../ui/icons'
import Logo from './Logo'

/**
 * Phase 0: logo, a search box (works from Phase 3), cart icon, dark mode and the phone menu.
 * Phases 2-3 add the account menu, the cart count and the role links (Seller, Admin).
 */
const links = [
  { to: '/', label: 'Home', end: true },
  { to: '/style-guide', label: 'Style guide' },
]

const linkClass = ({ isActive }) =>
  'rounded-lg px-3 py-2 text-sm font-medium transition-colors ' +
  (isActive
    ? 'bg-brand-50 text-brand-800 dark:bg-slate-800 dark:text-white'
    : 'text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white')

const iconButton = 'rounded-lg p-2 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'

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

function SearchBox({ className = '' }) {
  return (
    <form role="search" className={`relative ${className}`} onSubmit={(e) => e.preventDefault()}>
      <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
      <input type="search" name="q" placeholder="Search for products, brands and more" aria-label="Search products"
        className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm placeholder:text-slate-500 focus:border-brand-600 focus:bg-white dark:border-slate-700 dark:bg-slate-900 dark:placeholder:text-slate-400" />
    </form>
  )
}

export default function Navbar() {
  const [open, setOpen] = useState(false)
  const location = useLocation()
  const [lastPath, setLastPath] = useState(location.pathname)

  // close the phone menu after moving to another page
  if (location.pathname !== lastPath) {
    setLastPath(location.pathname)
    setOpen(false)
  }

  return (
    <header className="sticky top-0 z-40 border-b print:hidden border-slate-200 bg-white/90 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90">
      <nav className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4">
        <Logo />
        <SearchBox className="hidden flex-1 md:block" />
        <div className="hidden items-center gap-1 md:flex">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} className={linkClass}>{l.label}</NavLink>
          ))}
        </div>
        <div className="ml-auto flex items-center gap-1 md:ml-0">
          <ThemeToggle />
          <button type="button" className={iconButton} aria-label="Cart (coming in Phase 3)" title="Cart">
            <CartIcon />
          </button>
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
      {open && (
        <div className="border-t border-slate-200 px-4 py-3 md:hidden dark:border-slate-800">
          <div className="flex flex-col gap-1">
            {links.map((l) => (
              <NavLink key={l.to} to={l.to} end={l.end} className={linkClass}>{l.label}</NavLink>
            ))}
          </div>
        </div>
      )}
    </header>
  )
}
