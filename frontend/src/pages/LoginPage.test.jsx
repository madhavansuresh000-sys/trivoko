import { configureStore } from '@reduxjs/toolkit'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { loginRequest } from '../api/auth'
import { fetchCart, mergeCart } from '../api/cart'
import { guestCart } from '../lib/guestCart'
import authReducer from '../store/authSlice'
import cartReducer from '../store/cartSlice'
import notificationsReducer from '../store/notificationsSlice'
import LoginPage from './LoginPage'

// no real server in these tests: the calls are replaced by mocks we control
vi.mock('../api/auth', () => ({ loginRequest: vi.fn(), fetchMe: vi.fn(), logoutRequest: vi.fn(), registerRequest: vi.fn() }))
vi.mock('../api/cart')

const ravi = { id: 2, fullName: 'Ravi Kumar', email: 'ravi@trivoko.test', roles: ['CUSTOMER'], seller: null }
const cartOf = (itemCount) => ({ packages: [], itemCount, itemsTotal: 0, shippingTotal: 0, total: 0 })

function renderLogin(url = '/login') {
  const store = configureStore({
    reducer: { auth: authReducer, cart: cartReducer, notifications: notificationsReducer },
    preloadedState: { auth: { user: null, status: 'ready' } },
  })
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[url]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/cart" element={<p>Cart page</p>} />
          <Route path="/" element={<p>Home page</p>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  )
  return { user: userEvent.setup(), store }
}

async function fillAndSubmit(user, emailText, password) {
  await user.type(screen.getByLabelText('Email'), emailText)
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Log in' }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('shows what is missing and does not call the server', async () => {
    const { user } = renderLogin()
    await user.click(screen.getByRole('button', { name: 'Log in' }))
    expect(screen.getByText('Email is required')).toBeInTheDocument()
    expect(loginRequest).not.toHaveBeenCalled()
  })

  it('shows the server answer for a wrong password', async () => {
    vi.mocked(loginRequest).mockRejectedValue(Object.assign(new Error('401'),
      { response: { status: 401, data: { detail: 'Wrong email or password.' } } }))
    const { user } = renderLogin()
    await fillAndSubmit(user, 'ravi@trivoko.test', 'not-the-password')
    expect(await screen.findByRole('alert')).toHaveTextContent('Wrong email or password.')
  })

  it('tells a guest with a cart that logging in keeps it', () => {
    guestCart.add(57, 1)
    guestCart.add(12, 1)
    renderLogin()
    expect(screen.getByText('Log in to keep the 2 items in your cart.')).toBeInTheDocument()
  })

  // Phase 3 "done when": the guest cart is kept after login
  it('after login the guest cart is merged and the page goes to ?next=', async () => {
    guestCart.add(57, 2)
    vi.mocked(loginRequest).mockResolvedValue(ravi)
    vi.mocked(mergeCart).mockResolvedValue(cartOf(2))
    const { user, store } = renderLogin('/login?next=/cart')
    await fillAndSubmit(user, 'ravi@trivoko.test', 'secret123')

    expect(await screen.findByText('Cart page')).toBeInTheDocument()
    expect(mergeCart).toHaveBeenCalledWith([{ variantId: 57, quantity: 2 }])
    expect(fetchCart).not.toHaveBeenCalled()
    expect(guestCart.read()).toEqual([])
    expect(store.getState().cart.view.itemCount).toBe(2)
  })

  it('never follows ?next= to another website', async () => {
    vi.mocked(loginRequest).mockResolvedValue(ravi)
    vi.mocked(fetchCart).mockResolvedValue(cartOf(0))
    const { user } = renderLogin('/login?next=//evil.example')
    await fillAndSubmit(user, 'ravi@trivoko.test', 'secret123')
    expect(await screen.findByText('Home page')).toBeInTheDocument()
  })
})
