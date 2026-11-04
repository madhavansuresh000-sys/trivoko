import api from './client'

/** Product questions (after launch). Reading is public; asking needs a login; only the product's seller answers. */

/** -> page of { id, question, askedBy, createdAt, answer | null, answeredAt | null }, newest first */
export const fetchQuestions = (slug, page = 0) =>
  api.get(`/products/${encodeURIComponent(slug)}/questions`, { params: { page } }).then((r) => r.data)

/** question text (10-300 characters) -> the new question */
export const askQuestion = (slug, question) =>
  api.post(`/products/${encodeURIComponent(slug)}/questions`, { question }).then((r) => r.data)

/** The seller's list: answered=false waiting (oldest first), true answered -> page of { ..., productName, productSlug } */
export const fetchSellerQuestions = (answered, page = 0) =>
  api.get('/seller/questions', { params: { answered, page } }).then((r) => r.data)

/** answer text (2-1000 characters); answering again corrects the answer */
export const answerQuestion = (id, answer) => api.put(`/seller/questions/${id}/answer`, { answer }).then((r) => r.data)
