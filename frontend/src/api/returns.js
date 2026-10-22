import api from './client'

/**
 * Returns & refunds (Phase 8). The server checks every rule again (delivered, 7 days, once per line, whose turn
 * it is); the pages only offer the buttons that make sense right now.
 */

// ---- customer ----

/** { orderItemId, reason, note, photoUrl? } -> the new return (REQUESTED) */
export const requestReturn = (body) => api.post('/me/returns', body).then((r) => r.data)
export const fetchMyReturns = () => api.get('/me/returns').then((r) => r.data)
export const cancelReturn = (id) => api.post(`/me/returns/${id}/cancel`).then((r) => r.data)
export const escalateReturn = (id, note) => api.post(`/me/returns/${id}/escalate`, { note }).then((r) => r.data)

// ---- seller ----

/** status = '' (all) or REQUESTED / APPROVED / REJECTED / REFUNDED ... -> a page */
export const fetchSellerReturns = (status, page = 0) =>
  api.get('/seller/returns', { params: { ...(status ? { status } : {}), page } }).then((r) => r.data)
export const approveReturn = (id) => api.post(`/seller/returns/${id}/approve`).then((r) => r.data)
export const rejectReturn = (id, reason) => api.post(`/seller/returns/${id}/reject`, { reason }).then((r) => r.data)
/** The courier has it -> the customer is refunded at once (the answer says REFUNDED, or REFUND_FAILED) */
export const markReturnPickedUp = (id) => api.post(`/seller/returns/${id}/picked-up`).then((r) => r.data)

// ---- admin ----

/** status = ESCALATED (disputes) or REFUND_FAILED -> a page, oldest first */
export const fetchAdminReturns = (status, page = 0) =>
  api.get('/admin/returns', { params: { status, page } }).then((r) => r.data)
export const adminApproveReturn = (id) => api.post(`/admin/returns/${id}/approve`).then((r) => r.data)
export const adminCloseReturn = (id, reason) => api.post(`/admin/returns/${id}/close`, { reason }).then((r) => r.data)
export const retryReturnRefund = (id) => api.post(`/admin/returns/${id}/retry-refund`).then((r) => r.data)
