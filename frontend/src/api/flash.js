import api from './client'

/**
 * Flash sale (Phase 7). Times come from the SERVER (serverNow), so the countdown is right even when the phone's clock is wrong.
 */

/** The sale running now, or the next one: { id, title, startsAt, endsAt, serverNow, state, items } - or null when there is none (204) */
export const fetchCurrentSale = () => api.get('/flash-sales/current').then((r) => (r.status === 204 ? null : r.data))

/** One sale with the live "left" of every deal (the page asks again every few seconds) */
export const fetchSale = (id) => api.get(`/flash-sales/${id}`).then((r) => r.data)

/** Claim one unit -> { claimId, itemId, status: 'HELD', expiresAt }. 409 "Sold out!" / "already claimed", 429 slow down. */
export const claimDeal = (itemId) => api.post(`/flash-sales/items/${itemId}/claim`).then((r) => r.data)

/** My held (10 minutes) and bought deals */
export const fetchMyClaims = () => api.get('/me/flash-claims').then((r) => r.data)

// ---- admin ----
export const fetchAdminSales = () => api.get('/admin/flash-sales').then((r) => r.data)
export const createSale = (body) => api.post('/admin/flash-sales', body).then((r) => r.data)

// ---- seller ----
export const fetchOpenSales = () => api.get('/seller/flash-sales').then((r) => r.data)
export const fetchMyOffers = () => api.get('/seller/flash-sales/offers').then((r) => r.data)
export const offerDeal = (saleId, body) => api.post(`/seller/flash-sales/${saleId}/offers`, body).then((r) => r.data)
export const withdrawOffer = (itemId) => api.delete(`/seller/flash-sales/offers/${itemId}`)
