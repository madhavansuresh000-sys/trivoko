import api from './client'

/**
 * Login calls (copied from EventHub). The JWT travels in an httpOnly cookie that the browser stores and
 * sends by itself: our JavaScript never sees the token (so an injected script cannot steal it).
 * POST/PUT/DELETE carry the CSRF token automatically: Axios copies the XSRF-TOKEN cookie into the
 * X-XSRF-TOKEN header.
 */

/** The logged-in user, or null for a visitor (TriVoKo answers 401 to a visitor). */
export const fetchMe = () =>
  api.get('/auth/me').then((r) => r.data).catch((e) => {
    if (e?.response?.status === 401) return null
    throw e
  })

/** { email, password } -> the user (and the cookie is set) */
export const loginRequest = (credentials) => api.post('/auth/login', credentials).then((r) => r.data)

/** { fullName, email, password, phone } -> the new CUSTOMER, already logged in */
export const registerRequest = (form) => api.post('/auth/register', form).then((r) => r.data)

/** The server deletes the cookie. */
export const logoutRequest = () => api.post('/auth/logout')
