import api from './client'

/** The admin panel (Phase 6). Every URL is ADMIN only; the server checks again. Lists are paged (20 per page). */

const params = (obj) => Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== '' && v !== null && v !== undefined))

export const fetchOverview = () => api.get('/admin/overview').then((r) => r.data)

// ---- shops ----
/** status = '' (all) or PENDING / APPROVED / REJECTED / BLOCKED */
export const fetchShops = (status, q, page = 0) => api.get('/admin/sellers', { params: params({ status, q, page }) }).then((r) => r.data)
export const approveShop = (id) => api.post(`/admin/sellers/${id}/approve`).then((r) => r.data)
export const rejectShop = (id, reason) => api.post(`/admin/sellers/${id}/reject`, { reason }).then((r) => r.data)
export const blockShop = (id, reason) => api.post(`/admin/sellers/${id}/block`, { reason }).then((r) => r.data)
export const unblockShop = (id) => api.post(`/admin/sellers/${id}/unblock`).then((r) => r.data)

// ---- products ----
export const fetchAdminProducts = (status, q, page = 0) =>
  api.get('/admin/products', { params: params({ status, q, page }) }).then((r) => r.data)
export const fetchAdminProduct = (id) => api.get(`/admin/products/${id}`).then((r) => r.data)
export const approveProduct = (id) => api.post(`/admin/products/${id}/approve`).then((r) => r.data)
export const rejectProduct = (id, reason) => api.post(`/admin/products/${id}/reject`, { reason }).then((r) => r.data)

// ---- users ----
export const fetchUsers = (q, page = 0) => api.get('/admin/users', { params: params({ q, page }) }).then((r) => r.data)
export const blockUser = (id) => api.post(`/admin/users/${id}/block`).then((r) => r.data)
export const unblockUser = (id) => api.post(`/admin/users/${id}/unblock`).then((r) => r.data)

// ---- orders ----
/** q = part of an order number, or a customer's full email */
export const fetchAdminOrders = (q, status, page = 0) =>
  api.get('/admin/orders', { params: params({ q, status, page }) }).then((r) => r.data)
export const fetchAdminOrder = (number) => api.get(`/admin/orders/${encodeURIComponent(number)}`).then((r) => r.data)

// ---- coupons ----
export const fetchCoupons = () => api.get('/admin/coupons').then((r) => r.data)
/** { code, description, type: FLAT|PERCENT, value, maxDiscount, minOrder, validUntil } */
export const createCoupon = (coupon) => api.post('/admin/coupons', coupon).then((r) => r.data)
/** { description, value, maxDiscount, minOrder, validUntil } - code and type never change */
export const updateCoupon = (id, coupon) => api.put(`/admin/coupons/${id}`, coupon).then((r) => r.data)
export const endCoupon = (id) => api.post(`/admin/coupons/${id}/end`).then((r) => r.data)

// ---- reports + activity ----
/** days = 7, 30 or 90 */
export const fetchSalesReport = (days) => api.get('/admin/reports/sales', { params: { days } }).then((r) => r.data)
export const fetchTopProducts = (days) => api.get('/admin/reports/top-products', { params: { days } }).then((r) => r.data)
export const fetchTopSellers = (days) => api.get('/admin/reports/top-sellers', { params: { days } }).then((r) => r.data)
export const fetchActivity = (action, page = 0) => api.get('/admin/activity', { params: params({ action, page }) }).then((r) => r.data)
