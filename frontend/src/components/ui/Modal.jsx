import { useEffect, useRef } from 'react'

import { CloseIcon } from './icons'

/**
 * Pop-up dialog. Closes with the X button, the Escape key, or a click outside.
 * <Modal open={open} onClose={() => setOpen(false)} title="Cancel booking?">...</Modal>
 */
export default function Modal({ open, onClose, title, children, footer }) {
  const dialogRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    dialogRef.current?.focus()
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/60" onClick={onClose} aria-hidden="true" />
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        tabIndex={-1}
        className="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-xl outline-none dark:bg-slate-900"
      >
        <div className="flex items-start justify-between gap-4">
          <h2 id="modal-title" className="text-lg font-semibold text-slate-900 dark:text-white">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
            aria-label="Close"
          >
            <CloseIcon />
          </button>
        </div>
        <div className="mt-3 text-sm text-slate-600 dark:text-slate-300">{children}</div>
        {footer && <div className="mt-6 flex justify-end gap-3">{footer}</div>}
      </div>
    </div>
  )
}
