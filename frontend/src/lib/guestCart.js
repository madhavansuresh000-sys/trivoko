/**
 * A guest's cart, kept in this browser (localStorage) until they log in:
 *   [{ variantId: 57, quantity: 2 }, ...]
 * Only WHAT and HOW MANY - never prices. The prices on the cart page always come from the server
 * (POST /api/cart/preview), so editing localStorage cannot make anything cheaper.
 * After login the lines are sent to POST /api/cart/merge and this copy is cleared.
 */

export const GUEST_CART_KEY = 'trivoko.cart'
export const MAX_PER_LINE = 10
export const MAX_LINES = 30

const isLine = (l) =>
  l && Number.isInteger(l.variantId) && l.variantId > 0 && Number.isInteger(l.quantity) && l.quantity > 0

function read() {
  try {
    const parsed = JSON.parse(localStorage.getItem(GUEST_CART_KEY) ?? '[]')
    if (!Array.isArray(parsed)) return []
    // junk (an old version, a hand edit) is dropped instead of crashing the shop
    return parsed.filter(isLine).slice(0, MAX_LINES)
      .map((l) => ({ variantId: l.variantId, quantity: Math.min(l.quantity, MAX_PER_LINE) }))
  } catch {
    return [] // not JSON, or storage blocked (private window)
  }
}

function write(lines) {
  try {
    if (lines.length === 0) localStorage.removeItem(GUEST_CART_KEY)
    else localStorage.setItem(GUEST_CART_KEY, JSON.stringify(lines))
  } catch {
    // storage full or blocked: the cart simply is not remembered
  }
  return lines
}

function set(variantId, quantity) {
  const lines = read()
  const q = Math.max(1, Math.min(quantity, MAX_PER_LINE))
  const line = lines.find((l) => l.variantId === variantId)
  if (line) line.quantity = q
  else if (lines.length < MAX_LINES) lines.push({ variantId, quantity: q })
  return write(lines)
}

export const guestCart = {
  read,
  set,
  /** Adding the same size again adds to the quantity (as on the server). */
  add: (variantId, quantity) => {
    const current = read().find((l) => l.variantId === variantId)?.quantity ?? 0
    return set(variantId, current + quantity)
  },
  remove: (variantId) => write(read().filter((l) => l.variantId !== variantId)),
  clear: () => write([]),
}
