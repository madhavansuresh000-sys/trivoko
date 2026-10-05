import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'

import api from '../../api/client'
import { formatWhen } from '../../lib/dates'
import { BellIcon } from '../ui/icons'

/**
 * The bell (logged-in users): unread count + the newest messages, e.g. "Order TV-100123 is placed".
 * Refreshed when the page changes (no polling); opening the list marks everything as read.
 */
export default function NotificationBell() {
  const [bell, setBell] = useState({ unread: 0, items: [] })
  const [open, setOpen] = useState(false)
  const ref = useRef(null)
  const location = useLocation()
  const [lastPath, setLastPath] = useState(location.pathname)

  // close the list after moving to another page (decided while rendering, like the navbar's phone menu)
  if (location.pathname !== lastPath) {
    setLastPath(location.pathname)
    setOpen(false)
  }

  useEffect(() => {
    let current = true
    api.get('/notifications').then((r) => current && setBell(r.data)).catch(() => {})
    return () => {
      current = false
    }
  }, [location.pathname])

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

  const toggle = () => {
    setOpen((o) => !o)
    if (!open && bell.unread > 0) {
      api.post('/notifications/read-all').catch(() => {})
      setBell((b) => ({ unread: 0, items: b.items.map((i) => ({ ...i, read: true })) }))
    }
  }

  return (
    <div className="relative" ref={ref}>
      <button type="button" onClick={toggle} aria-expanded={open}
        aria-label={`Notifications${bell.unread ? `, ${bell.unread} unread` : ''}`}
        className="relative rounded-lg p-2 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800">
        <BellIcon />
        {bell.unread > 0 && (
          <span aria-hidden="true" className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-xs font-bold text-white">
            {bell.unread > 9 ? '9+' : bell.unread}
          </span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 top-full z-50 mt-1 w-80 max-w-[calc(100vw-2rem)] rounded-xl border border-slate-200 bg-white p-2 shadow-lg dark:border-slate-700 dark:bg-slate-900">
          <p className="px-2 pb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Notifications</p>
          {bell.items.length === 0 ? (
            <p className="px-2 py-4 text-center text-sm text-slate-500 dark:text-slate-400">Nothing yet.</p>
          ) : (
            <ul className="max-h-96 overflow-y-auto">
              {bell.items.map((n) => {
                const body = (
                  <>
                    <p className="text-sm font-semibold text-slate-900 dark:text-white">{n.title}</p>
                    <p className="line-clamp-2 text-xs text-slate-600 dark:text-slate-400">{n.body}</p>
                    <p className="mt-0.5 text-[11px] text-slate-400">{formatWhen(n.createdAt)}</p>
                  </>
                )
                return (
                  <li key={n.id}>
                    {n.link
                      ? <Link to={n.link} className="block rounded-lg px-2 py-2 hover:bg-slate-100 dark:hover:bg-slate-800">{body}</Link>
                      : <div className="px-2 py-2">{body}</div>}
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
