/** The same rules as the server (ReviewRequest): 1-5 stars, title up to 80, text 10-1000 characters. */
export function checkReview({ rating, title, body }) {
  const errors = {}
  if (!rating) errors.rating = 'Pick 1 to 5 stars'
  if ((title ?? '').length > 80) errors.title = 'The title can be at most 80 characters'
  const text = (body ?? '').trim()
  if (text.length < 10 || text.length > 1000) errors.body = 'The review must be 10 to 1000 characters'
  return errors
}
