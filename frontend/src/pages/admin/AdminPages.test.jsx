import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as admin from '../../api/admin'
import authReducer from '../../store/authSlice'
import notificationsReducer from '../../store/notificationsSlice'
import { toRequest } from '../../lib/couponForm'
import AdminCouponsPage from './AdminCouponsPage'
import AdminLayout from './AdminLayout'
import AdminShopsPage from './AdminShopsPage'

vi.mock('../../api/admin')

const ADMIN = { id: 1, email: 'admin@trivoko.test', fullName: 'TriVoKo Admin', roles: ['ADMIN', 'CUSTOMER'], seller: null }
const RAVI = { id: 2, email: 'ravi@trivoko.test', fullName: 'Ravi Kumar', roles: ['CUSTOMER'], seller: null }

function renderWith(ui, user = ADMIN) {
  const store = configureStore({
    reducer: { auth: authReducer, notifications: notificationsReducer },
    preloadedState: { auth: { user, status: 'ready' } },
  })
  render(<Provider store={store}><MemoryRouter>{ui}</MemoryRouter></Provider>)
  return userEvent.setup()
}

const page = (content) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true })

const erode = { id: 9, ownerId: 12, shopName: 'Erode Organics', slug: 'erode-organics', city: 'Erode',
  description: 'Organic home items', gstin: null, status: 'PENDING', createdAt: '2026-10-01T10:00:00' }

describe('AdminLayout', () => {
  it('shows the admin menu to the admin', () => {
    renderWith(<Routes><Route path="*" element={<AdminLayout />} /></Routes>)
    expect(screen.getByRole('navigation', { name: 'Admin menu' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Activity log' })).toBeInTheDocument()
  })

  it('tells a customer the panel is not for them', () => {
    renderWith(<Routes><Route path="*" element={<AdminLayout />} /></Routes>, RAVI)
    expect(screen.queryByRole('navigation', { name: 'Admin menu' })).not.toBeInTheDocument()
    expect(screen.getByText('This page is for the TriVoKo team')).toBeInTheDocument()
  })
})

describe('AdminShopsPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('approving a pending shop calls the API', async () => {
    admin.fetchShops.mockResolvedValue(page([erode]))
    admin.approveShop.mockResolvedValue({ ...erode, status: 'APPROVED' })
    const user = renderWith(<AdminShopsPage />)
    await user.click(await screen.findByRole('button', { name: 'Approve' }))
    expect(admin.approveShop).toHaveBeenCalledWith(9)
  })

  it('rejecting asks for a real reason first', async () => {
    admin.fetchShops.mockResolvedValue(page([erode]))
    admin.rejectShop.mockResolvedValue({ ...erode, status: 'REJECTED' })
    const user = renderWith(<AdminShopsPage />)
    await user.click(await screen.findByRole('button', { name: 'Reject' }))
    const reason = await screen.findByLabelText('Reason (the seller sees it)')
    await user.type(reason, 'no')
    await user.click(screen.getAllByRole('button', { name: 'Reject' }).at(-1))
    expect(admin.rejectShop).not.toHaveBeenCalled()
    expect(screen.getByText(/5 to 300 characters/)).toBeInTheDocument()

    await user.type(reason, ' GST number is missing')
    await user.click(screen.getAllByRole('button', { name: 'Reject' }).at(-1))
    await waitFor(() => expect(admin.rejectShop).toHaveBeenCalledWith(9, 'no GST number is missing'))
  })

  it('a blocked shop shows its reason and an Unblock button', async () => {
    admin.fetchShops.mockResolvedValue(page([{ ...erode, status: 'BLOCKED', blockReason: 'Fake goods' }]))
    renderWith(<AdminShopsPage />)
    expect(await screen.findByText('Blocked: Fake goods')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Unblock' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Block' })).not.toBeInTheDocument()
  })
})

describe('AdminCouponsPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('turns the form into the API request', () => {
    const form = { code: ' diwali20 ', description: '20% off', type: 'PERCENT', value: '20', maxDiscount: '', minOrder: '', validUntil: '2026-11-30' }
    expect(toRequest(form, false)).toEqual({ code: 'DIWALI20', type: 'PERCENT', description: '20% off', value: 20,
      maxDiscount: null, minOrder: 0, validUntil: '2026-11-30T23:59:59' })
    expect(toRequest({ ...form, type: 'FLAT', maxDiscount: '300' }, true)).not.toHaveProperty('code')
  })

  it('ending a coupon calls the API', async () => {
    admin.fetchCoupons.mockResolvedValue([{ id: 2, code: 'FLAT100', description: '₹100 off', type: 'FLAT', value: 100,
      maxDiscount: null, minOrder: 999, validFrom: '2026-10-01T00:00:00', validUntil: '2027-10-01T00:00:00', active: true }])
    admin.endCoupon.mockResolvedValue({})
    const user = renderWith(<AdminCouponsPage />)
    await user.click(await screen.findByRole('button', { name: 'End now' }))
    expect(admin.endCoupon).toHaveBeenCalledWith(2)
  })

  it('a new coupon needs a code, a value and a last day', async () => {
    admin.fetchCoupons.mockResolvedValue([])
    const user = renderWith(<AdminCouponsPage />)
    await user.click(await screen.findByRole('button', { name: 'New coupon' }))
    await user.click(screen.getByRole('button', { name: 'Create coupon' }))
    expect(admin.createCoupon).not.toHaveBeenCalled()
    expect(screen.getByText('3 to 30 letters or digits')).toBeInTheDocument()
    expect(screen.getByText('Choose the last day')).toBeInTheDocument()
  })
})
