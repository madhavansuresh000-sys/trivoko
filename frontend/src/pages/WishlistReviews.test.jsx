import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as wishlistApi from '../api/wishlist'
import RatingSummary from '../components/review/RatingSummary'
import ReviewForm from '../components/review/ReviewForm'
import WishlistButton from '../components/wishlist/WishlistButton'
import authReducer from '../store/authSlice'
import notificationsReducer from '../store/notificationsSlice'
import wishlistReducer from '../store/wishlistSlice'
import WishlistPage from './WishlistPage'

vi.mock('../api/wishlist')

/** Phase 9 step 6 - the heart, the review form rules, the rating bars and the wishlist page. */

const ravi = { id: 2, email: 'ravi@trivoko.test', fullName: 'Ravi Kumar', roles: ['CUSTOMER'], seller: null }

function renderWith(ui, { user = null, saved = [] } = {}) {
  const store = configureStore({
    reducer: { auth: authReducer, wishlist: wishlistReducer, notifications: notificationsReducer },
    preloadedState: { auth: { user, status: 'ready' }, wishlist: { ids: saved } },
  })
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/products/volta-v12-5g']}>
        <Routes>
          <Route path="/products/:slug" element={ui} />
          <Route path="/login" element={<p>Login page</p>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  )
  return { user: userEvent.setup(), store }
}

const volta = { id: 1, name: 'Volta V12 5G' }

describe('WishlistButton', () => {
  beforeEach(() => vi.resetAllMocks())

  it('sends a guest to log in', async () => {
    const { user } = renderWith(<WishlistButton product={volta} />)
    await user.click(screen.getByRole('button', { name: 'Save Volta V12 5G to wishlist' }))
    expect(await screen.findByText('Login page')).toBeInTheDocument()
    expect(wishlistApi.addToWishlist).not.toHaveBeenCalled()
  })

  it('fills and empties the heart for a customer', async () => {
    wishlistApi.addToWishlist.mockResolvedValue({})
    wishlistApi.removeFromWishlist.mockResolvedValue({})
    const { user, store } = renderWith(<WishlistButton product={volta} />, { user: ravi })

    await user.click(screen.getByRole('button', { name: 'Save Volta V12 5G to wishlist' }))
    expect(wishlistApi.addToWishlist).toHaveBeenCalledWith(1)
    expect(await screen.findByRole('button', { name: 'Remove Volta V12 5G from wishlist' })).toHaveAttribute('aria-pressed', 'true')
    expect(store.getState().wishlist.ids).toEqual([1])

    await user.click(screen.getByRole('button', { name: 'Remove Volta V12 5G from wishlist' }))
    expect(wishlistApi.removeFromWishlist).toHaveBeenCalledWith(1)
    await waitFor(() => expect(store.getState().wishlist.ids).toEqual([]))
  })

  it('puts the heart back when the server says no', async () => {
    wishlistApi.addToWishlist.mockRejectedValue({ response: { status: 409, data: { detail: 'Your wishlist is full' } } })
    const { user, store } = renderWith(<WishlistButton product={volta} />, { user: ravi })
    await user.click(screen.getByRole('button', { name: 'Save Volta V12 5G to wishlist' }))
    await waitFor(() => expect(store.getState().wishlist.ids).toEqual([]))
    expect(store.getState().notifications[0].text).toBe('Your wishlist is full')
  })
})

describe('ReviewForm', () => {
  it('needs a rating and at least 10 characters before it calls the server', async () => {
    const onSave = vi.fn().mockResolvedValue()
    const { user } = renderWith(<ReviewForm initial={null} onSave={onSave} />)

    await user.click(screen.getByRole('button', { name: 'Post review' }))
    expect(screen.getByText('Pick 1 to 5 stars')).toBeInTheDocument()
    expect(screen.getByText('The review must be 10 to 1000 characters')).toBeInTheDocument()
    expect(onSave).not.toHaveBeenCalled()

    await user.click(screen.getByText('4 stars - Good'))
    await user.type(screen.getByLabelText('Your review'), 'Too short')
    await user.click(screen.getByRole('button', { name: 'Post review' }))
    expect(onSave).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText('Your review'), ' - good grip though')
    await user.click(screen.getByRole('button', { name: 'Post review' }))
    await waitFor(() => expect(onSave).toHaveBeenCalledWith({ rating: 4, title: null, body: 'Too short - good grip though' }))
  })
})

describe('RatingSummary', () => {
  it('shows the average, the count and one bar per star', () => {
    renderWith(<RatingSummary summary={{ average: 4.3, count: 3, stars: { 5: 1, 4: 2, 3: 0, 2: 0, 1: 0 } }} />)
    expect(screen.getByText('4.3')).toBeInTheDocument()
    expect(screen.getByText('3 reviews')).toBeInTheDocument()
    expect(screen.getByLabelText('4 stars: 2')).toBeInTheDocument()
    expect(screen.getByLabelText('1 stars: 0')).toBeInTheDocument()
  })

  it('says so when there are no reviews', () => {
    renderWith(<RatingSummary summary={{ average: null, count: 0, stars: {} }} />)
    expect(screen.getByText('No reviews yet.')).toBeInTheDocument()
  })
})

describe('WishlistPage', () => {
  beforeEach(() => vi.resetAllMocks())

  const card = (id, name, extra = {}) => ({
    id, slug: `p-${id}`, name, brand: 'Volta', priceFrom: 11999, mrpFrom: 14499, discountPercent: 17, imageUrl: null,
    inStock: true, sellerName: 'Chennai Mobiles', sellerSlug: 'chennai-mobiles', categorySlug: 'phones',
    ratingAvg: null, ratingCount: 0, ...extra,
  })

  it('shows how much cheaper a product is now, and marks one that is gone', async () => {
    wishlistApi.fetchWishlist.mockResolvedValue([
      { product: card(1, 'Volta V12 5G'), available: true, priceWhenAdded: 12999, priceDrop: 1000, addedAt: '2026-10-20T10:00:00' },
      { product: card(2, 'Old Phone'), available: false, priceWhenAdded: 5999, priceDrop: 0, addedAt: '2026-10-19T10:00:00' },
    ])
    renderWith(<WishlistPage />, { user: ravi, saved: [1, 2] })
    expect(await screen.findByText('₹1,000 lower than when you saved it (₹12,999)')).toBeInTheDocument()
    expect(screen.getByText('Not available')).toBeInTheDocument()
  })

  it('an empty wishlist invites you to browse', async () => {
    wishlistApi.fetchWishlist.mockResolvedValue([])
    renderWith(<WishlistPage />, { user: ravi })
    expect(await screen.findByText('Nothing saved yet')).toBeInTheDocument()
  })
})
