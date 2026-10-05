import api from './client'

/** "My account": delivery addresses and the "become a seller" application. */

export const fetchAddresses = () => api.get('/me/addresses').then((r) => r.data)

/** address = { name, phone, line1, line2, city, state, pincode } */
export const createAddress = (address) => api.post('/me/addresses', address).then((r) => r.data)

export const updateAddress = (id, address) => api.put(`/me/addresses/${id}`, address).then((r) => r.data)

export const makeDefaultAddress = (id) => api.put(`/me/addresses/${id}/default`).then((r) => r.data)

export const deleteAddress = (id) => api.delete(`/me/addresses/${id}`)

/** My shop application, or null if I never applied (the server answers 404). */
export const fetchMyApplication = () =>
  api.get('/seller/application').then((r) => r.data).catch((e) => {
    if (e?.response?.status === 404) return null
    throw e
  })

/** { shopName, city, description, gstin } -> the application, status PENDING */
export const applyAsSeller = (form) => api.post('/seller/apply', form).then((r) => r.data)
