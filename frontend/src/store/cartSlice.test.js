import { configureStore } from '@reduxjs/toolkit'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as cartApi from '../api/cart'
import { guestCart } from '../lib/guestCart'
import authReducer from './authSlice'
import cartReducer, { addToCart, loadCart, mergeGuestCart, selectCartCount } from './cartSlice'

vi.mock('../api/cart')

const view = (itemCount, lines = []) => ({
  packages: lines.length ? [{ seller: { name: 'Chennai Mobiles', slug: 'chennai-mobiles' }, items: lines, itemsTotal: 0, shippingFee: 0 }] : [],
  itemCount, itemsTotal: 0, shippingTotal: 0, total: 0,
})

function makeStore(user = null) {
  return configureStore({
    reducer: { auth: authReducer, cart: cartReducer },
    preloadedState: { auth: { user, status: 'ready' } },
  })
}

const ravi = { id: 2, fullName: 'Ravi Kumar', roles: ['CUSTOMER'], seller: null }

describe('cartSlice', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.resetAllMocks()
  })

  it('a guest add is kept in the browser and priced by the server', async () => {
    cartApi.previewCart.mockResolvedValue(view(2))
    const store = makeStore()
    await store.dispatch(addToCart({ variantId: 57, quantity: 2 }))

    expect(guestCart.read()).toEqual([{ variantId: 57, quantity: 2 }])
    expect(cartApi.previewCart).toHaveBeenCalledWith([{ variantId: 57, quantity: 2 }])
    expect(cartApi.putCartItem).not.toHaveBeenCalled()
    expect(selectCartCount(store.getState())).toBe(2)
  })

  it('a logged-in add sets the server quantity to what is there + the new ones', async () => {
    cartApi.fetchCart.mockResolvedValue(view(1, [{ variantId: 57, quantity: 1, available: true }]))
    cartApi.putCartItem.mockResolvedValue(view(3))
    const store = makeStore(ravi)
    await store.dispatch(loadCart())
    await store.dispatch(addToCart({ variantId: 57, quantity: 2 }))

    expect(cartApi.putCartItem).toHaveBeenCalledWith(57, 3)
    expect(guestCart.read()).toEqual([])
    expect(selectCartCount(store.getState())).toBe(3)
  })

  it('after login the guest lines are merged and the browser copy is cleared', async () => {
    guestCart.add(57, 1)
    guestCart.add(12, 2)
    cartApi.mergeCart.mockResolvedValue(view(3))
    const store = makeStore(ravi)
    await store.dispatch(mergeGuestCart())

    expect(cartApi.mergeCart).toHaveBeenCalledWith([{ variantId: 57, quantity: 1 }, { variantId: 12, quantity: 2 }])
    expect(guestCart.read()).toEqual([])
    expect(selectCartCount(store.getState())).toBe(3)
  })

  it('an empty guest cart is not merged - the saved cart is just loaded', async () => {
    cartApi.fetchCart.mockResolvedValue(view(4))
    const store = makeStore(ravi)
    await store.dispatch(mergeGuestCart())

    expect(cartApi.mergeCart).not.toHaveBeenCalled()
    expect(selectCartCount(store.getState())).toBe(4)
  })

  it('a guest with an empty cart does not call the server at all', async () => {
    const store = makeStore()
    await store.dispatch(loadCart())
    expect(cartApi.previewCart).not.toHaveBeenCalled()
    expect(selectCartCount(store.getState())).toBe(0)
  })

  it('a 401 on a write logs the user out of the app', async () => {
    cartApi.fetchCart.mockResolvedValue(view(0))
    cartApi.putCartItem.mockRejectedValue({ response: { status: 401, data: { detail: 'Please log in' } } })
    const store = makeStore(ravi)
    const result = await store.dispatch(addToCart({ variantId: 57, quantity: 1 }))

    expect(result.payload.message).toBe('Please log in again.')
    expect(store.getState().auth.user).toBeNull()
  })
})
