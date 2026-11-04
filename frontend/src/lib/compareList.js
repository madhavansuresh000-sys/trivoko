/**
 * "Compare" (after launch): the two products this browser picked to compare, as slugs.
 *   localStorage 'trivoko.compare' = ['volta-v12-5g', 'kaveri-k5']
 * A third pick pushes out the oldest, like a two-slot shelf. The page itself always loads fresh data.
 */

export const COMPARE_KEY = 'trivoko.compare'
export const MAX_COMPARE = 2

const isSlug = (s) => typeof s === 'string' && /^[a-z0-9-]{1,120}$/.test(s)

function read() {
  try {
    const parsed = JSON.parse(localStorage.getItem(COMPARE_KEY) ?? '[]')
    return Array.isArray(parsed) ? [...new Set(parsed.filter(isSlug))].slice(-MAX_COMPARE) : []
  } catch {
    return []
  }
}

function write(slugs) {
  try {
    localStorage.setItem(COMPARE_KEY, JSON.stringify(slugs))
  } catch {
    // storage blocked: the choice is simply not remembered
  }
  return slugs
}

export const compareList = {
  read,
  has: (slug) => read().includes(slug),
  /** Adds the product (pushing out the oldest of two), or removes it if it is already there. */
  toggle: (slug) => {
    const slugs = read()
    return write(slugs.includes(slug) ? slugs.filter((s) => s !== slug) : [...slugs, slug].slice(-MAX_COMPARE))
  },
  clear: () => write([]),
}

/** The link to the compare page; the slugs are in the URL so a comparison can be shared. */
export const compareLink = (slugs) => `/compare?p=${slugs.map(encodeURIComponent).join(',')}`

/** Slugs from ?p=a,b (only valid ones, at most two). */
export const slugsFromQuery = (value) => [...new Set((value ?? '').split(',').filter(isSlug))].slice(0, MAX_COMPARE)
