import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as flash from '../api/flash'
import authReducer from '../store/authSlice'
import cartReducer from '../store/cartSlice'
import notificationsReducer from '../store/notificationsSlice'
import FlashSalePage from './FlashSalePage'

vi.mock('../api/flash')

const deal = (over = {}) => ({ itemId: 1, variantId: 10, productSlug: 'volta-v12-5g', productName: 'Volta V12 5G', variantLabel: '8GB / 128GB',
  imageUrl: null, shopName: 'Chennai Mobiles', salePrice: 9999, mrp: 14499, quantity: 100, left: 40, soldOut: false, ...over })
const sale = (items, over = {}) => ({ id: 5, title: 'Midnight Deals', startsAt: '2026-10-15T20:00:00', endsAt: '2026-10-15T22:00:00',
  serverNow: '2026-10-15T21:00:00', state: 'LIVE', items, ...over })
const ravi = { id: 2, fullName: 'Ravi Kumar', email: 'ravi@trivoko.test', roles: ['CUSTOMER'], seller: null }

function renderPage(user = ravi) {
  const store = configureStore({
    reducer: { auth: authReducer, cart: cartReducer, notifications: notificationsReducer },
    preloadedState: { auth: { user, status: 'ready' } },
  })
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/flash']}>
        <Routes>
          <Route path="/flash" element={<FlashSalePage />} />
          <Route path="/login" element={<p>Login page</p>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  )
}

describe('FlashSalePage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    flash.fetchMyClaims.mockResolvedValue([])
    flash.fetchSale.mockResolvedValue(sale([deal()]))
  })

  it('shows the deal with the sale price and the live "left" counter', async () => {
    flash.fetchCurrentSale.mockResolvedValue(sale([deal()]))
    renderPage()
    expect(await screen.findByText('Volta V12 5G')).toBeInTheDocument()
    expect(screen.getByText('₹9,999')).toBeInTheDocument()
    expect(screen.getByText('40 of 100 left')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Claim now' })).toBeEnabled()
  })

  it('a sold-out deal cannot be claimed', async () => {
    flash.fetchCurrentSale.mockResolvedValue(sale([deal({ left: 0, soldOut: true })]))
    renderPage()
    expect(await screen.findByText('Sold out', { selector: 'p' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sold out' })).toBeDisabled()
  })

  it('claiming calls the server once and tells the person', async () => {
    flash.fetchCurrentSale.mockResolvedValue(sale([deal()]))
    flash.claimDeal.mockResolvedValue({ claimId: 9, itemId: 1, status: 'HELD' })
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Claim now' }))
    await waitFor(() => expect(flash.claimDeal).toHaveBeenCalledWith(1))
    expect(flash.claimDeal).toHaveBeenCalledTimes(1)
  })

  it('a visitor who is not logged in is sent to the login page', async () => {
    flash.fetchCurrentSale.mockResolvedValue(sale([deal()]))
    renderPage(null)
    await userEvent.click(await screen.findByRole('button', { name: 'Claim now' }))
    expect(await screen.findByText('Login page')).toBeInTheDocument()
    expect(flash.claimDeal).not.toHaveBeenCalled()
  })

  it('an upcoming sale shows "Not started" and no claim', async () => {
    flash.fetchCurrentSale.mockResolvedValue(sale([deal()], { state: 'UPCOMING', startsAt: '2026-10-15T23:00:00' }))
    renderPage()
    expect(await screen.findByRole('button', { name: 'Not started' })).toBeDisabled()
  })

  it('says so when there is no sale at all', async () => {
    flash.fetchCurrentSale.mockResolvedValue(null)
    renderPage()
    expect(await screen.findByText('No flash sale right now')).toBeInTheDocument()
  })
})
