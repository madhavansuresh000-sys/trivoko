import { beforeEach, describe, expect, it } from 'vitest'

import { GUEST_CART_KEY, guestCart } from './guestCart'

describe('guestCart', () => {
  beforeEach(() => localStorage.clear())

  it('starts empty', () => {
    expect(guestCart.read()).toEqual([])
  })

  it('adding the same variant twice adds the quantities', () => {
    guestCart.add(57, 1)
    guestCart.add(57, 2)
    guestCart.add(12, 1)
    expect(guestCart.read()).toEqual([{ variantId: 57, quantity: 3 }, { variantId: 12, quantity: 1 }])
  })

  it('never goes above 10 per line', () => {
    guestCart.add(57, 8)
    guestCart.add(57, 8)
    expect(guestCart.read()).toEqual([{ variantId: 57, quantity: 10 }])
  })

  it('set changes a quantity and remove drops the line', () => {
    guestCart.add(57, 1)
    guestCart.set(57, 4)
    expect(guestCart.read()).toEqual([{ variantId: 57, quantity: 4 }])
    guestCart.remove(57)
    expect(guestCart.read()).toEqual([])
  })

  it('keeps at most 30 different lines', () => {
    for (let id = 1; id <= 31; id++) guestCart.add(id, 1)
    expect(guestCart.read()).toHaveLength(30)
  })

  // Review focus: someone edits localStorage by hand - the shop must not crash
  it('guestCart ignores junk', () => {
    localStorage.setItem(GUEST_CART_KEY, 'abc')
    expect(guestCart.read()).toEqual([])
    localStorage.setItem(GUEST_CART_KEY, JSON.stringify({ variantId: 1 }))
    expect(guestCart.read()).toEqual([])
    localStorage.setItem(GUEST_CART_KEY, JSON.stringify([{ variantId: 'x', quantity: 1 }, { variantId: 5, quantity: -2 },
      null, { variantId: 7, quantity: 2.5 }, { variantId: 9, quantity: 50 }]))
    expect(guestCart.read()).toEqual([{ variantId: 9, quantity: 10 }])
  })

  it('clear empties it', () => {
    guestCart.add(57, 1)
    guestCart.clear()
    expect(guestCart.read()).toEqual([])
  })
})
