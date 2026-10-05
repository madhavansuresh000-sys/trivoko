import { useEffect, useState } from 'react'

import { fetchCategories } from '../api/catalog'

// The tree hardly ever changes, so it is fetched ONCE per visit and shared by every component
// (navbar menu, home tiles, filters, placeholder art). A failed fetch is forgotten, so the next use retries.
let cached = null

function load() {
  if (!cached) {
    cached = fetchCategories().catch((e) => {
      cached = null
      throw e
    })
  }
  return cached
}

/** [] until loaded (or if the server is down - menus simply stay empty). */
export default function useCategories() {
  const [tree, setTree] = useState([])
  useEffect(() => {
    let current = true
    load().then((t) => current && setTree(t)).catch(() => {})
    return () => {
      current = false
    }
  }, [])
  return tree
}
