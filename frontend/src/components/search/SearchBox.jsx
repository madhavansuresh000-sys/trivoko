import { useEffect, useId, useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'

import { fetchSuggestions } from '../../api/search'
import { SearchIcon } from '../ui/icons'

/** Wait this long after the last key before asking the server (typing "iphone" = 1 request, not 6). */
const SUGGEST_DELAY_MS = 250
const MIN_LETTERS = 2

/**
 * The search box with suggestions while typing (Phase 10). Keyboard: ↓ ↑ move, Enter opens, Esc closes.
 * Built as an ARIA "combobox" so a screen reader says "3 suggestions, Volta, brand".
 *
 *   type "vo" ──250 ms quiet──► GET /api/search/suggest?q=vo ──► [Volta (brand), Volta V12 5G, ...]
 *   Enter with nothing chosen ──► /products?q=vo (the full smart search)
 */
export default function SearchBox({ className = '' }) {
  const [params] = useSearchParams()
  const location = useLocation()
  // show the current search while on the listing; empty elsewhere. key = start fresh when it changes.
  const current = location.pathname === '/products' ? params.get('q') ?? '' : ''
  return <Combobox key={current} initial={current} className={className} />
}

function Combobox({ initial, className }) {
  const navigate = useNavigate()
  const listId = useId()
  const [text, setText] = useState(initial)
  const [suggestions, setSuggestions] = useState([])
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const timer = useRef(null)
  const latest = useRef('')

  useEffect(() => () => clearTimeout(timer.current), [])

  const type = (value) => {
    setText(value)
    setActive(-1)
    clearTimeout(timer.current)
    const q = value.trim()
    latest.current = q
    if (q.length < MIN_LETTERS) {
      setSuggestions([])
      setOpen(false)
      return
    }
    timer.current = setTimeout(() => {
      fetchSuggestions(q)
        .then((list) => {
          if (latest.current !== q) return // the customer typed more meanwhile: this answer is old
          setSuggestions(list)
          setOpen(list.length > 0)
        })
        .catch(() => setSuggestions([])) // suggestions are a help, never an error message
    }, SUGGEST_DELAY_MS)
  }

  const close = () => {
    setOpen(false)
    setActive(-1)
  }

  const choose = (s) => {
    close()
    navigate(s.type === 'product' ? `/products/${s.slug}` : `/products?brand=${encodeURIComponent(s.text)}`)
  }

  const submit = (e) => {
    e.preventDefault()
    if (open && active >= 0) {
      choose(suggestions[active])
      return
    }
    close()
    const q = text.trim()
    navigate(q ? `/products?q=${encodeURIComponent(q)}` : '/products')
  }

  const keys = (e) => {
    if (e.key === 'Escape') {
      close()
    } else if ((e.key === 'ArrowDown' || e.key === 'ArrowUp') && suggestions.length > 0) {
      e.preventDefault()
      setOpen(true)
      const step = e.key === 'ArrowDown' ? 1 : -1
      setActive((a) => (a + step + suggestions.length) % suggestions.length)
    }
  }

  const optionId = (i) => `${listId}-${i}`
  const showList = open && suggestions.length > 0

  return (
    <form role="search" className={`relative ${className}`} onSubmit={submit}>
      <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
      <input type="search" name="q" value={text} onChange={(e) => type(e.target.value)} onKeyDown={keys} onBlur={close}
        placeholder="Search for products, brands and more" aria-label="Search products" enterKeyHint="search"
        role="combobox" aria-autocomplete="list" aria-expanded={showList} aria-controls={listId}
        aria-activedescendant={showList && active >= 0 ? optionId(active) : undefined} autoComplete="off"
        className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm placeholder:text-slate-500 focus:border-brand-600 focus:bg-white dark:border-slate-700 dark:bg-slate-900 dark:placeholder:text-slate-400" />
      {showList && (
        <ul id={listId} role="listbox" aria-label="Suggestions"
          className="absolute inset-x-0 top-full z-30 mt-1 overflow-hidden rounded-lg border border-slate-200 bg-white py-1 shadow-lg dark:border-slate-700 dark:bg-slate-900">
          {suggestions.map((s, i) => (
            <li key={`${s.type}-${s.slug ?? s.text}`} id={optionId(i)} role="option" aria-selected={i === active}
              // mousedown (not click) fires before the input's blur closes the list
              onMouseDown={(e) => { e.preventDefault(); choose(s) }}
              className={`flex cursor-pointer items-center justify-between gap-2 px-3 py-2 text-sm ${
                i === active ? 'bg-brand-50 text-brand-800 dark:bg-slate-800 dark:text-brand-200' : 'text-slate-700 dark:text-slate-200'}`}>
              <span className="truncate">{s.text}</span>
              {s.type === 'brand' && <span className="shrink-0 text-xs text-slate-500 dark:text-slate-400">Brand</span>}
            </li>
          ))}
        </ul>
      )}
    </form>
  )
}
