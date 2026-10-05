import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'

import { deleteCartItem, fetchCart, mergeCart, previewCart, putCartItem } from '../api/cart'
import { describeError } from '../hooks/useAsync'
import { guestCart } from '../lib/guestCart'
import { logout, sessionExpired } from './authSlice'

/**
 * The cart, the same for guests and customers on the screen, different underneath:
 *
 *   guest      : lines in localStorage (lib/guestCart) --POST /cart/preview--> view
 *   logged in  : lines on the server              --GET/PUT/DELETE /cart--> view
 *   login      : POST /cart/merge (guest lines join the saved cart), then the browser copy is cleared
 *
 * state.view is always the server's last answer: prices, stock and packages are never worked out here.
 */

export const EMPTY_VIEW = Object.freeze({ packages: [], itemCount: 0, itemsTotal: 0, shippingTotal: 0, total: 0 })

const isLoggedIn = (getState) => Boolean(getState().auth.user)

/** Runs a cart call; a 401 means the 8-hour login ran out -> forget the user, ask to log in again. */
async function run(call, { dispatch, rejectWithValue }) {
  try {
    return await call()
  } catch (e) {
    if (e?.response?.status === 401) {
      dispatch(sessionExpired())
      return rejectWithValue({ status: 401, message: 'Please log in again.', fieldErrors: {} })
    }
    return rejectWithValue(describeError(e))
  }
}

const guestView = () => {
  const lines = guestCart.read()
  return lines.length ? previewCart(lines) : Promise.resolve(EMPTY_VIEW)
}

export const loadCart = createAsyncThunk('cart/load', (_, api) =>
  run(() => (isLoggedIn(api.getState) ? fetchCart() : guestView()), api))

/** "Add to cart" on the product page: adds to what is already there (the server caps at 10 and the stock). */
export const addToCart = createAsyncThunk('cart/add', ({ variantId, quantity }, api) =>
  run(async () => {
    if (!isLoggedIn(api.getState)) {
      guestCart.add(variantId, quantity)
      return guestView()
    }
    // PUT sets the quantity, so we must know what is already saved - if the cart has not loaded yet,
    // ask the server first (otherwise "add 1" could overwrite a saved 4 with 1)
    const view = api.getState().cart.view ?? await fetchCart()
    const lines = (view.packages ?? []).flatMap((p) => p.items)
    const current = lines.find((l) => l.variantId === variantId)?.quantity ?? 0
    return putCartItem(variantId, Math.min(current + quantity, 10))
  }, api))

export const setCartQuantity = createAsyncThunk('cart/setQuantity', ({ variantId, quantity }, api) =>
  run(() => {
    if (!isLoggedIn(api.getState)) {
      guestCart.set(variantId, quantity)
      return guestView()
    }
    return putCartItem(variantId, quantity)
  }, api))

export const removeFromCart = createAsyncThunk('cart/remove', (variantId, api) =>
  run(() => {
    if (!isLoggedIn(api.getState)) {
      guestCart.remove(variantId)
      return guestView()
    }
    return deleteCartItem(variantId)
  }, api))

/** Right after login / register. */
export const mergeGuestCart = createAsyncThunk('cart/merge', (_, api) =>
  run(async () => {
    const lines = guestCart.read()
    if (lines.length === 0) return fetchCart()
    const merged = await mergeCart(lines)
    guestCart.clear() // only after the server has them
    return merged
  }, api))

const cartSlice = createSlice({
  name: 'cart',
  initialState: { view: null, status: 'idle', error: null },
  reducers: {},
  extraReducers: (builder) => {
    const pending = (state) => {
      state.status = 'loading'
      state.error = null
    }
    const fulfilled = (state, action) => {
      state.view = action.payload
      state.status = 'ready'
    }
    const rejected = (state, action) => {
      state.status = 'error'
      state.error = action.payload?.message ?? 'Something went wrong.'
    }
    for (const thunk of [loadCart, addToCart, setCartQuantity, removeFromCart, mergeGuestCart]) {
      builder.addCase(thunk.pending, pending).addCase(thunk.fulfilled, fulfilled).addCase(thunk.rejected, rejected)
    }
    // the saved cart stays on the server for the next login; this browser starts empty again
    builder.addCase(logout.fulfilled, () => ({ view: EMPTY_VIEW, status: 'ready', error: null }))
  },
})

export default cartSlice.reducer

export const selectCart = (state) => state.cart
export const selectCartView = (state) => state.cart.view ?? EMPTY_VIEW
export const selectCartCount = (state) => state.cart.view?.itemCount ?? 0
export const selectCartLines = (state) => (state.cart.view?.packages ?? []).flatMap((p) => p.items)
