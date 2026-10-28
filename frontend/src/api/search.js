import api from './client'

/** Smart search (Phase 10). Public - no login needed. */

/** params = URLSearchParams (same names as the listing + sort=relevance) -> { results: page of cards, facets } */
export const searchProducts = (params) => api.get('/search', { params }).then((r) => r.data)

/** What the customer has typed so far -> [{ type: 'brand'|'product', text, slug }] (empty below 2 letters) */
export const fetchSuggestions = (q) => api.get('/search/suggest', { params: { q } }).then((r) => r.data)

/** Up to 6 cards bought in the same paid orders as this product */
export const fetchAlsoBought = (slug) =>
  api.get(`/products/${encodeURIComponent(slug)}/also-bought`).then((r) => r.data)
