import api from './client'

/** The seller's back office (Phase 5). Every URL needs an APPROVED shop; the server checks ownership again. */

export const fetchDashboard = () => api.get('/seller/dashboard').then((r) => r.data)

/** status = '' (all) or PLACED / PACKED / SHIPPED / DELIVERED / CANCELLED */
export const fetchSellerPackages = (status, page = 0) =>
  api.get('/seller/packages', { params: { ...(status ? { status } : {}), page } }).then((r) => r.data)

export const packPackage = (id) => api.post(`/seller/packages/${id}/pack`).then((r) => r.data)
export const shipPackage = (id, trackingNumber) => api.post(`/seller/packages/${id}/ship`, { trackingNumber }).then((r) => r.data)
export const deliverPackage = (id) => api.post(`/seller/packages/${id}/deliver`).then((r) => r.data)
export const cancelPackage = (id, reason) => api.post(`/seller/packages/${id}/cancel`, { reason }).then((r) => r.data)

export const fetchLedger = (page = 0) => api.get('/seller/ledger', { params: { page } }).then((r) => r.data)

/** days = 7, 30 or 90 -> [{ date, total, packages }] oldest first */
export const fetchSales = (days) => api.get('/seller/sales', { params: { days } }).then((r) => r.data)

export const fetchLowStock = () => api.get('/seller/products/low-stock').then((r) => r.data)

// ---- products ----

export const fetchMyProducts = (status, page = 0) =>
  api.get('/seller/products', { params: { ...(status ? { status } : {}), page, size: 48 } }).then((r) => r.data)

export const fetchMyProduct = (id) => api.get(`/seller/products/${id}`).then((r) => r.data)

/** { name, categoryId, brand, description, variants: [{ id?, label, size, colour, price, mrp, stock }] } */
export const createProduct = (product) => api.post('/seller/products', product).then((r) => r.data)
export const updateProduct = (id, product) => api.put(`/seller/products/${id}`, product).then((r) => r.data)
export const submitProduct = (id) => api.post(`/seller/products/${id}/submit`).then((r) => r.data)

/** The whole photo list in its order: [{ url, altText }] (max 8) */
export const saveProductImages = (id, photos) => api.put(`/seller/products/${id}/images`, photos).then((r) => r.data)
