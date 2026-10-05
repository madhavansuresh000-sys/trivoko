import api from './client'

/**
 * Checkout, payment and my orders (Phase 4). The server recalculates every amount; the page only shows them.
 */

/** { addressId?, couponCode? } -> { address, packages, itemsTotal, discountTotal, shippingTotal, grandTotal, coupon, unavailable } */
export const previewCheckout = (body) => api.post('/checkout/preview', body).then((r) => r.data)

/** { addressId, couponCode?, expectedTotal } -> { number, grandTotal, redirectUrl } (409 if prices changed) */
export const placeOrder = (body) => api.post('/orders', body).then((r) => r.data)

export const fetchOrders = (page = 0) => api.get('/orders', { params: { page } }).then((r) => r.data)

export const fetchOrder = (number) => api.get(`/orders/${encodeURIComponent(number)}`).then((r) => r.data)

// ---- payment pages ----

/** The built-in TEST payment page (development, no Stripe key): { orderNumber, amount, status } */
export const fetchTestPayment = (session) => api.get(`/payments/fake/${encodeURIComponent(session)}`).then((r) => r.data)

/** Its "Pay" button -> the order (PAID) */
export const completeTestPayment = (session) =>
  api.post(`/payments/fake/${encodeURIComponent(session)}/complete`).then((r) => r.data)

/** Back from Stripe: ask the server to check with Stripe now (the webhook may be late) -> the order */
export const verifyPayment = (session) => api.post(`/payments/${encodeURIComponent(session)}/verify`).then((r) => r.data)
