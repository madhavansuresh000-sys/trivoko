import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as account from '../api/account'
import * as orders from '../api/orders'
import authReducer from '../store/authSlice'
import cartReducer from '../store/cartSlice'
import notificationsReducer from '../store/notificationsSlice'
import CheckoutPage from './CheckoutPage'

vi.mock('../api/account')
vi.mock('../api/orders')
vi.mock('../api/cart')

const address = { id: 7, name: 'Ravi Kumar', phone: '9000000002', line1: '12 Anna Salai', line2: null,
  city: 'Chennai', state: 'Tamil Nadu', pincode: '600002', isDefault: true }

const item = (variantId, name, unitPrice, quantity, share = 0) => ({ variantId, productSlug: `p-${variantId}`,
  productName: name, variantLabel: 'Black', unitPrice, mrp: unitPrice, quantity, lineTotal: unitPrice * quantity, discountShare: share })

const preview = (overrides = {}) => ({
  address,
  packages: [
    { sellerName: 'Chennai Mobiles', sellerSlug: 'chennai-mobiles', items: [item(1, 'Silicone case', 499, 2, 99.8)],
      itemsTotal: 998, discount: 99.8, shippingFee: 0, total: 898.2 },
    { sellerName: 'Kovai Sports', sellerSlug: 'kovai-sports', items: [item(2, 'Kovai Run Pro', 2499, 1, 249.9)],
      itemsTotal: 2499, discount: 249.9, shippingFee: 0, total: 2249.1 },
  ],
  itemsTotal: 3497, discountTotal: 349.7, shippingTotal: 0, grandTotal: 3147.3,
  coupon: { code: 'WELCOME10', discount: 349.7, error: null }, unavailable: [],
  ...overrides,
})

/** The cart the customer last saw: the case cost Rs 549 then (now 499). */
const cartView = {
  packages: [{ seller: { name: 'Chennai Mobiles', slug: 'chennai-mobiles' }, items: [
    { variantId: 1, productName: 'Silicone case', price: 549, quantity: 2, available: true },
    { variantId: 2, productName: 'Kovai Run Pro', price: 2499, quantity: 1, available: true }] }],
  itemCount: 3, itemsTotal: 0, shippingTotal: 0, total: 0,
}

function renderCheckout() {
  const store = configureStore({
    reducer: { auth: authReducer, cart: cartReducer, notifications: notificationsReducer },
    preloadedState: {
      auth: { user: { id: 2, fullName: 'Ravi Kumar', roles: ['CUSTOMER'] }, status: 'ready' },
      cart: { view: cartView, status: 'ready', error: null },
    },
  })
  render(<Provider store={store}><MemoryRouter><CheckoutPage /></MemoryRouter></Provider>)
  return userEvent.setup()
}

describe('CheckoutPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    account.fetchAddresses.mockResolvedValue([address])
    orders.previewCheckout.mockResolvedValue(preview())
  })

  it('shows one box per seller and the total on the Pay button', async () => {
    renderCheckout()
    expect(await screen.findByText(/Package 1 · Chennai Mobiles/)).toBeInTheDocument()
    expect(screen.getByText(/Package 2 · Kovai Sports/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Pay ₹3,147.30/ })).toBeEnabled()
  })

  it('warns when a price changed since the cart was shown', async () => {
    renderCheckout()
    expect(await screen.findByText(/Price changed: Silicone case is now ₹499 \(was ₹549\)/)).toBeInTheDocument()
  })

  it('shows why a coupon was not applied', async () => {
    orders.previewCheckout.mockResolvedValue(preview({ coupon: { code: 'NOPE', discount: 0, error: 'This coupon does not exist' } }))
    renderCheckout()
    expect(await screen.findByText('This coupon does not exist')).toBeInTheDocument()
  })

  it('sends the total the customer saw, and goes to the payment page', async () => {
    orders.placeOrder.mockResolvedValue({ number: 'TV-100123', grandTotal: 3147.3, redirectUrl: 'http://localhost:5173/test-payment/fake_cs_1' })
    const assign = vi.fn()
    vi.stubGlobal('location', { ...window.location, assign })
    const user = renderCheckout()
    await user.click(await screen.findByRole('button', { name: /Pay ₹3,147.30/ }))
    await waitFor(() => expect(orders.placeOrder).toHaveBeenCalledWith({ addressId: 7, couponCode: undefined, expectedTotal: 3147.3 }))
    expect(assign).toHaveBeenCalledWith('http://localhost:5173/test-payment/fake_cs_1')
    vi.unstubAllGlobals()
  })
})
