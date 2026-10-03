import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import { dismissed, selectNotifications } from '../../store/notificationsSlice'
import { CloseIcon } from '../ui/icons'

const SHOW_FOR_MS = 4000

const tones = {
  success: 'border-green-200 bg-white text-slate-800 dark:border-green-900 dark:bg-slate-900 dark:text-slate-100',
  error: 'border-red-300 bg-red-50 text-red-900 dark:border-red-900 dark:bg-red-950 dark:text-red-200',
}

function Toast({ toast }) {
  const dispatch = useDispatch()

  // each toast removes itself after a few seconds
  useEffect(() => {
    const timer = setTimeout(() => dispatch(dismissed(toast.id)), SHOW_FOR_MS)
    return () => clearTimeout(timer)
  }, [toast.id, dispatch])

  return (
    <div className={`pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-lg ${tones[toast.tone] ?? tones.success}`}>
      <span aria-hidden="true">{toast.tone === 'error' ? '⚠️' : '✅'}</span>
      <p className="flex-1">{toast.text}</p>
      <button type="button" onClick={() => dispatch(dismissed(toast.id))} aria-label="Dismiss"
        className="rounded p-0.5 text-slate-400 hover:text-slate-700 dark:hover:text-white">
        <CloseIcon className="h-4 w-4" />
      </button>
    </div>
  )
}

/** Shows the messages from the notifications slice at the bottom of the screen. */
export default function Toaster() {
  const toasts = useSelector(selectNotifications)
  return (
    <div aria-live="polite" className="pointer-events-none fixed inset-x-4 bottom-4 z-50 flex flex-col items-center gap-2 print:hidden sm:left-auto sm:right-4 sm:items-end">
      {toasts.map((t) => (
        <div key={t.id} className="w-full max-w-sm"><Toast toast={t} /></div>
      ))}
    </div>
  )
}
