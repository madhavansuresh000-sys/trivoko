import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as cartApi from '../api/cart'
import * as catalog from '../api/catalog'
import * as reviewsApi from '../api/reviews'
import * as searchApi from '../api/search'
import RequireAuth from '../components/auth/RequireAuth'
import authReducer from '../store/authSlice'
import cartReducer from '../store/cartSlice'
import notificationsReducer from '../store/notificationsSlice'
import wishlistReducer from '../store/wishlistSlice'
import CartPage from './CartPage'
import ProductPage from './ProductPage'
import RegisterPage from './RegisterPage'

vi.mock('../api/auth')
vi.mock('../api/cart')
vi.mock('../api/catalog')
vi.mock('../api/reviews')
vi.mock('../api/search')

/** Phase 11 step 6 - the main screens that had no test of their own: guard, register, cart, product page. */

const ravi = { id: 2, email: 'ravi@trivoko.test', fullName: 'Ravi Kumar', roles: ['CUSTOMER'], seller: null }

function Where() {
  const location = useLocation()
  return <p data-testid="where">{location.pathname + location.search}</p>
}

function renderAt(url, routes, { user = null, status = 'ready' } = {}) {
  const store = configureStore({
    reducer: { auth: authReducer, cart: cartReducer, wishlist: wishlistReducer, notifications: notificationsReducer },
    preloadedState: { auth: { user, status } },
  })
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[url]}>
        <Routes>{routes}</Routes>
        <Where />
      </MemoryRouter>
    </Provider>,
  )
  return { user: userEvent.setup(), store }
}

beforeEach(() => {
  vi.resetAllMocks()
  catalog.fetchCategories.mockResolvedValue([]) // the navbar-style category hook some pages use
})

describe('RequireAuth', () => {
  const guarded = (allow) => (
    <>
      <Route element={<RequireAuth allow={allow} what="the admin panel" />}>
        <Route path="/admin" element={<p>Admin page</p>} />
      </Route>
      <Route path="/login" element={<p>Login page</p>} />
    </>
  )

  it('sends a guest to log in, and remembers where they wanted to go', () => {
    renderAt('/admin?tab=users', guarded())
    expect(screen.getByText('Login page')).toBeInTheDocument()
    expect(screen.getByTestId('where')).toHaveTextContent('/login?next=%2Fadmin%3Ftab%3Dusers')
  })

  it('waits while the app still asks the server who you are', () => {
    renderAt('/admin', guarded(), { status: 'checking' })
    expect(screen.getByRole('status')).toHaveTextContent('Checking your login')
  })

  it('shows "no permission" to a customer on an admin page, and the page itself to the right person', () => {
    renderAt('/admin', guarded((u) => u.roles.includes('ADMIN')), { user: ravi })
    expect(screen.getByText('You cannot open this page')).toBeInTheDocument()
    expect(screen.queryByText('Admin page')).not.toBeInTheDocument()
  })
})

describe('RegisterPage', () => {
  it('checks the form before anything is sent', async () => {
    const { user } = renderAt('/register', <Route path="/register" element={<RegisterPage />} />)
    await user.click(screen.getByRole('button', { name: /Create (my )?account/ }))
    expect(screen.getByText('Full name is required')).toBeInTheDocument()
    expect(screen.getByText('Email is required')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Full name'), 'Ravi Kumar')
    await user.type(screen.getByLabelText('Email'), 'ravi@gmail.com')
    await user.type(screen.getByLabelText('Mobile number (optional)'), '12345')
    await user.type(screen.getByLabelText('Password', { exact: true }), 'onlyletters')
    await user.type(screen.getByLabelText('Confirm password'), 'different1')
    await user.click(screen.getByRole('button', { name: /Create (my )?account/ }))

    expect(screen.getByText('Phone must be 10 digits, e.g. 9876543210')).toBeInTheDocument()
    expect(screen.getByText('Use both letters and numbers')).toBeInTheDocument()
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument()
    expect(authApi.registerRequest).not.toHaveBeenCalled()
  })
})

describe('CartPage', () => {
  const line = (variantId, productName, quantity = 1) => ({
    variantId, productSlug: `p-${variantId}`, productName, variantLabel: 'Default', imageUrl: null, price: 299, mrp: 399,
    quantity, maxQuantity: 10, available: true, note: null,
  })
  const twoShops = {
    packages: [
      { seller: { name: 'Chennai Mobiles', slug: 'chennai-mobiles' }, items: [line(1, 'Volta V12 5G')], itemsTotal: 12999, shippingFee: 0 },
      { seller: { name: 'Pondy Books', slug: 'pondy-books' }, items: [line(2, 'Letters from Pondicherry')], itemsTotal: 299, shippingFee: 40 },
    ],
    itemCount: 2, itemsTotal: 13298, shippingTotal: 40, total: 13338,
  }

  it('one cart, one package per shop, each with its own delivery rule', async () => {
    cartApi.fetchCart.mockResolvedValue(twoShops)
    renderAt('/cart', <Route path="/cart" element={<CartPage />} />, { user: ravi })

    expect(await screen.findByRole('region', { name: 'Package 1 from Chennai Mobiles' })).toHaveTextContent('Free delivery')
    expect(screen.getByRole('region', { name: 'Package 2 from Pondy Books' })).toHaveTextContent('Delivery ₹40 · free from ₹499')
    expect(screen.getByText(/one from each seller/)).toBeInTheDocument()
  })

  it('"one more" saves the new quantity on the server', async () => {
    cartApi.fetchCart.mockResolvedValue(twoShops)
    cartApi.putCartItem.mockResolvedValue(twoShops)
    const { user } = renderAt('/cart', <Route path="/cart" element={<CartPage />} />, { user: ravi })
    await screen.findByRole('region', { name: 'Package 1 from Chennai Mobiles' })

    await user.click(screen.getAllByRole('button', { name: 'One more' })[0])
    await waitFor(() => expect(cartApi.putCartItem).toHaveBeenCalledWith(1, 2))
  })
})

describe('ProductPage', () => {
  const product = (variants, extra = {}) => ({
    id: 1, slug: 'volta-v12-5g', name: 'Volta V12 5G', brand: 'Volta', description: 'A good phone.',
    priceFrom: 11999, mrpFrom: 14499, discountPercent: 17,
    category: { name: 'Mobile phones', slug: 'phones', parent: { name: 'Mobiles & Accessories', slug: 'mobiles-accessories', parent: null } },
    seller: { shopName: 'Chennai Mobiles', slug: 'chennai-mobiles', city: 'Chennai' },
    variants, images: [], ratingAvg: 4.5, ratingCount: 12, lowestPrice30Days: 11999, lowestNow: true, ...extra,
  })
  const variant = (id, inStock) => ({
    id, sku: `V-${id}`, label: 'Black / 128 GB', size: null, colour: 'Black', price: 11999, mrp: 14499,
    discountPercent: 17, inStock, onlyLeft: null,
  })

  beforeEach(() => {
    reviewsApi.fetchReviews.mockResolvedValue({ summary: { average: 4.5, count: 12, stars: {} }, reviews: { content: [], page: 0, totalPages: 0 } })
    searchApi.fetchAlsoBought.mockResolvedValue([])
  })

  const open = () => renderAt('/products/volta-v12-5g', <Route path="/products/:slug" element={<ProductPage />} />)

  it('shows the stars, the price and the "lowest in 30 days" badge', async () => {
    catalog.fetchProduct.mockResolvedValue(product([variant(1, true)]))
    open()
    expect(await screen.findByRole('heading', { name: 'Volta V12 5G' })).toBeInTheDocument()
    // the link under the name (the reviews section further down says it too, once it has loaded)
    expect(screen.getByRole('link', { name: /4.5 · 12 reviews/ })).toHaveAttribute('href', '#reviews')
    expect(screen.getByText('Lowest in 30 days')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Add to cart/ })).toBeEnabled()
  })

  it('a sold-out product cannot be added to the cart', async () => {
    catalog.fetchProduct.mockResolvedValue(product([variant(1, false)], { lowestNow: false }))
    open()
    expect(await screen.findByRole('button', { name: 'Sold out', exact: true })).toBeDisabled()
    expect(screen.queryByText('Lowest in 30 days')).not.toBeInTheDocument()
  })

  it('an unknown product shows the not-found page', async () => {
    catalog.fetchProduct.mockRejectedValue({ response: { status: 404, data: { detail: 'Product not found' } } })
    open()
    expect((await screen.findAllByText(/not found|does not exist|404/i)).length).toBeGreaterThan(0)
    expect(screen.queryByRole('heading', { name: 'Volta V12 5G' })).not.toBeInTheDocument()
  })
})
