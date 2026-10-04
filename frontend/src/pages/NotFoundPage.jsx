import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import api from '../api/client'
import Button from '../components/ui/Button'
import EmptyShelfArt from '../components/ui/EmptyShelfArt'
import { ArrowLeftIcon, SearchIcon } from '../components/ui/icons'

const POPULAR_COUNT = 6

/**
 * The "helpful shop 404" (Phase 2 spec, section 8): instead of a dead end, it offers a search box,
 * a few real categories and a way back. The categories come from GET /api/categories; if that call
 * fails the row is simply left out - this page must never break.
 */
export default function NotFoundPage() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [categories, setCategories] = useState([])
  // "Go back" only makes sense when the visitor came from another page of ours
  const canGoBack = typeof window !== 'undefined' && window.history.length > 1

  useEffect(() => {
    const previousTitle = document.title
    document.title = 'Page not found - TriVoKo'
    return () => {
      document.title = previousTitle
    }
  }, [])

  useEffect(() => {
    let active = true // ignore a late answer if the visitor already left this page
    api
      .get('/categories')
      .then((response) => {
        if (active && Array.isArray(response.data)) {
          setCategories(response.data.slice(0, POPULAR_COUNT))
        }
      })
      .catch(() => {}) // no categories = no "Popular" row, nothing else changes
    return () => {
      active = false
    }
  }, [])

  function search(event) {
    event.preventDefault()
    const text = query.trim()
    if (text) {
      navigate(`/products?${new URLSearchParams({ q: text })}`)
    }
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center py-12 text-center sm:py-16">
      <EmptyShelfArt className="w-56 sm:w-64" />

      <p className="mt-6 text-6xl font-extrabold text-brand-700 dark:text-brand-400">404</p>
      <h1 className="mt-3 text-2xl font-bold text-slate-900 dark:text-white">This shelf is empty</h1>
      <p className="mt-2 max-w-md text-slate-600 dark:text-slate-400">
        The link may be wrong, or the product may have been removed.
      </p>

      <form onSubmit={search} role="search" className="mt-8 flex w-full max-w-md gap-2">
        <label htmlFor="notfound-search" className="sr-only">Search products</label>
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
          <input
            id="notfound-search"
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search products..."
            className="w-full rounded-lg border border-slate-300 bg-white py-2 pl-10 pr-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-600/30 dark:border-slate-600 dark:bg-slate-900 dark:text-white"
          />
        </div>
        <Button type="submit">Search</Button>
      </form>

      {categories.length > 0 && (
        <div className="mt-6 flex flex-wrap items-center justify-center gap-2">
          <span className="text-sm font-medium text-slate-500 dark:text-slate-400">Popular:</span>
          {categories.map((category) => (
            <Link
              key={category.id}
              to={`/products?${new URLSearchParams({ category: category.slug })}`}
              className="rounded-full border border-brand-200 px-3 py-1 text-sm font-medium text-brand-800 hover:bg-brand-50 dark:border-slate-700 dark:text-brand-300 dark:hover:bg-slate-800"
            >
              {category.name}
            </Link>
          ))}
        </div>
      )}

      <div className="mt-10 flex flex-wrap justify-center gap-3">
        <Button to="/">Go home</Button>
        {canGoBack && (
          <Button variant="secondary" onClick={() => navigate(-1)}>
            <ArrowLeftIcon className="h-4 w-4" /> Go back
          </Button>
        )}
      </div>
    </div>
  )
}
