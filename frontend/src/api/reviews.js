import api from './client'

/** Reviews (Phase 9). Reading is public; "mine" needs a login. The server checks "delivered to you" again. */

/** -> { summary: { average, count, stars: { 5: n, 4: n, ... } }, reviews: page of { id, rating, title, body, author, ... } } */
export const fetchReviews = (slug, page = 0) =>
  api.get(`/products/${encodeURIComponent(slug)}/reviews`, { params: { page } }).then((r) => r.data)

/** -> { canReview, review | null } */
export const fetchMyReview = (slug) => api.get(`/products/${encodeURIComponent(slug)}/reviews/mine`).then((r) => r.data)

/** { rating, title, body } -> the saved review (the first save creates it, later saves edit it) */
export const saveMyReview = (slug, body) =>
  api.put(`/products/${encodeURIComponent(slug)}/reviews/mine`, body).then((r) => r.data)

export const deleteMyReview = (slug) => api.delete(`/products/${encodeURIComponent(slug)}/reviews/mine`)
