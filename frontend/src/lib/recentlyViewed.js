/**
 * "Recently viewed" (after launch): the ids of the last products this browser opened, newest first.
 *   localStorage 'trivoko.recent' = [42, 17, 3]
 * Only ids - the cards (prices, stock) always come fresh from GET /api/products/cards, like the guest cart.
 */

export const RECENT_KEY = 'trivoko.recent'
export const MAX_RECENT = 12

function read() {
  try {
    const parsed = JSON.parse(localStorage.getItem(RECENT_KEY) ?? '[]')
    return Array.isArray(parsed) ? parsed.filter((id) => Number.isInteger(id) && id > 0).slice(0, MAX_RECENT) : []
  } catch {
    return [] // not JSON, or storage blocked (private window)
  }
}

/** Opening a product moves it to the front; the oldest falls off after 12. */
function add(productId) {
  const ids = [productId, ...read().filter((id) => id !== productId)].slice(0, MAX_RECENT)
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(ids))
  } catch {
    // storage full or blocked: simply not remembered
  }
  return ids
}

export const recentlyViewed = { read, add }
