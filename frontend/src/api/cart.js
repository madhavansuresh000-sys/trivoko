import api from './client'

/**
 * The cart. Every call answers with the WHOLE cart view:
 *   { packages: [{ seller, items: [...], itemsTotal, shippingFee }], itemCount, itemsTotal, shippingTotal, total }
 * so the page just shows the latest answer.
 */

/** A guest's cart from localStorage, priced by the server (nothing saved). lines = [{ variantId, quantity }] */
export const previewCart = (lines) => api.post('/cart/preview', { items: lines }).then((r) => r.data)

export const fetchCart = () => api.get('/cart').then((r) => r.data)

export const putCartItem = (variantId, quantity) =>
  api.put(`/cart/items/${variantId}`, { quantity }).then((r) => r.data)

export const deleteCartItem = (variantId) => api.delete(`/cart/items/${variantId}`).then((r) => r.data)

/** After login: add the guest lines to my saved cart. */
export const mergeCart = (lines) => api.post('/cart/merge', { items: lines }).then((r) => r.data)
