import api from './client'

/** My wishlist (Phase 9). PUT = add, so pressing the heart twice is harmless. */

/** -> [{ product: ProductCard, available, priceWhenAdded, priceDrop, addedAt }] */
export const fetchWishlist = () => api.get('/me/wishlist').then((r) => r.data)
export const fetchWishlistIds = () => api.get('/me/wishlist/ids').then((r) => r.data)
export const addToWishlist = (productId) => api.put(`/me/wishlist/${productId}`)
export const removeFromWishlist = (productId) => api.delete(`/me/wishlist/${productId}`)
