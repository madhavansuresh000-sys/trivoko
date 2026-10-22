import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as seller from '../../api/seller'
import notificationsReducer from '../../store/notificationsSlice'
import SellerPackagesPage from './SellerPackagesPage'

vi.mock('../../api/seller')

const box = (status, extra = {}) => ({ id: 22, orderNumber: 'TV-100123', status, placedAt: '2026-10-05T10:42:00',
  items: [{ productName: 'Kovai Run Pro', variantLabel: 'UK 8', quantity: 1 }], total: 2249.1,
  trackingNumber: null, cancelReason: null, shipName: 'Ravi Kumar', shipCity: 'Chennai', shipPincode: '600002', ...extra })

const page = (content) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true })

function renderPage() {
  const store = configureStore({ reducer: { notifications: notificationsReducer } })
  render(<Provider store={store}><MemoryRouter><SellerPackagesPage /></MemoryRouter></Provider>)
  return userEvent.setup()
}

describe('SellerPackagesPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('a placed package offers "Mark packed"', async () => {
    seller.fetchSellerPackages.mockResolvedValue(page([box('PLACED')]))
    seller.packPackage.mockResolvedValue(box('PACKED'))
    const user = renderPage()
    await user.click(await screen.findByRole('button', { name: 'Mark packed' }))
    expect(seller.packPackage).toHaveBeenCalledWith(22)
  })

  it('shipping asks for the tracking number first', async () => {
    seller.fetchSellerPackages.mockResolvedValue(page([box('PACKED')]))
    seller.shipPackage.mockResolvedValue(box('SHIPPED', { trackingNumber: 'AWB12345' }))
    const user = renderPage()
    await user.click(await screen.findByRole('button', { name: 'Ship' }))
    const input = await screen.findByLabelText('Tracking number (AWB)')
    await user.click(screen.getByRole('button', { name: 'Mark shipped' }))
    expect(seller.shipPackage).not.toHaveBeenCalled() // empty -> the form complains first
    await user.type(input, 'AWB12345')
    await user.click(screen.getByRole('button', { name: 'Mark shipped' }))
    await waitFor(() => expect(seller.shipPackage).toHaveBeenCalledWith(22, 'AWB12345'))
  })

  it('a shipped package shows its tracking number and no cancel button', async () => {
    seller.fetchSellerPackages.mockResolvedValue(page([box('SHIPPED', { trackingNumber: 'AWB12345' })]))
    renderPage()
    expect(await screen.findByText(/AWB12345/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument()
  })
})
