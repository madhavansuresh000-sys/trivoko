import api from './client'

/** The public catalogue (no login needed). */

/** params = URLSearchParams from toApiParams() -> { content, totalElements, totalPages, page, ... } */
export const fetchProducts = (params) => api.get('/products', { params }).then((r) => r.data)

export const fetchProduct = (slug) => api.get(`/products/${encodeURIComponent(slug)}`).then((r) => r.data)

/** [{ brand, count }] for the brand checkboxes; category '' = the whole shop */
export const fetchBrands = (category) =>
  api.get('/products/brands', { params: category ? { category } : {} }).then((r) => r.data)

/** The category tree: [{ id, name, slug, children: [...] }] */
export const fetchCategories = () => api.get('/categories').then((r) => r.data)

/** A shop page header: { shopName, slug, city, description, sellingSince } */
export const fetchSeller = (slug) => api.get(`/sellers/${encodeURIComponent(slug)}`).then((r) => r.data)
