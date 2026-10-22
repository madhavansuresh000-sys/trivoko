import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as returnsApi from '../api/returns'
import ReturnRequestDialog from '../components/returns/ReturnRequestDialog'
import notificationsReducer from '../store/notificationsSlice'
import AdminReturnsPage from './admin/AdminReturnsPage'
import MyReturnsPage from './MyReturnsPage'
import SellerReturnsPage from './seller/SellerReturnsPage'

vi.mock('../api/returns')

/** Phase 8 step 9 - the return pages offer only the moves that are the viewer's to make. */

function renderWith(ui) {
  const store = configureStore({ reducer: { notifications: notificationsReducer } })
  render(<Provider store={store}><MemoryRouter>{ui}</MemoryRouter></Provider>)
  return userEvent.setup()
}

const ret = (status, extra = {}) => ({
  id: 7, orderItemId: 31, orderId: 4, orderNumber: 'TV-100123', productName: 'Kovai Run Pro', variantLabel: 'UK 8',
  quantity: 1, sellerName: 'Kovai Sports', status, reason: 'DAMAGED', note: 'The sole came apart',
  photoUrl: null, refundAmount: 2249.1, rejectReason: null, createdAt: '2026-10-22T10:00:00',
  events: [{ status: 'REQUESTED', actor: 'CUSTOMER', note: 'The sole came apart', at: '2026-10-22T10:00:00' }],
  ...extra,
})

const page = (content) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true })

describe('ReturnRequestDialog', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows the refund (line minus coupon share) and needs a reason and a note', async () => {
    returnsApi.requestReturn.mockResolvedValue(ret('REQUESTED'))
    const onDone = vi.fn()
    const item = { id: 31, productName: 'Kovai Run Pro', variantLabel: 'UK 8', quantity: 1, lineTotal: 2499, discountShare: 249.9 }
    const user = renderWith(<ReturnRequestDialog item={item} onClose={() => {}} onDone={onDone} />)

    expect(screen.getByText('₹2,249.10')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Ask for a return' }))
    expect(screen.getByText('Please choose a reason')).toBeInTheDocument()
    expect(returnsApi.requestReturn).not.toHaveBeenCalled()

    await user.selectOptions(screen.getByLabelText('Why are you returning it?'), 'DAMAGED')
    await user.type(screen.getByLabelText('Tell the seller what is wrong'), 'The sole came apart')
    await user.click(screen.getByRole('button', { name: 'Ask for a return' }))
    await waitFor(() => expect(returnsApi.requestReturn).toHaveBeenCalledWith(
      { orderItemId: 31, reason: 'DAMAGED', note: 'The sole came apart' }))
    expect(onDone).toHaveBeenCalled()
  })
})

describe('MyReturnsPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('a waiting return can be cancelled', async () => {
    returnsApi.fetchMyReturns.mockResolvedValue([ret('REQUESTED')])
    returnsApi.cancelReturn.mockResolvedValue(ret('CANCELLED'))
    const user = renderWith(<MyReturnsPage />)
    await user.click(await screen.findByRole('button', { name: 'Cancel return' }))
    expect(returnsApi.cancelReturn).toHaveBeenCalledWith(7)
    expect(screen.queryByRole('button', { name: 'Ask TriVoKo to review' })).not.toBeInTheDocument()
  })

  it('a rejected return shows the reason and can be sent for review once', async () => {
    returnsApi.fetchMyReturns.mockResolvedValue([ret('REJECTED', { rejectReason: 'Worn outdoors' })])
    returnsApi.escalateReturn.mockResolvedValue(ret('ESCALATED'))
    const user = renderWith(<MyReturnsPage />)
    expect(await screen.findByText(/Worn outdoors/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Ask TriVoKo to review' }))
    await user.type(await screen.findByLabelText(/Reason/), 'I have photos of the box')
    await user.click(screen.getByRole('button', { name: 'Send for review' }))
    await waitFor(() => expect(returnsApi.escalateReturn).toHaveBeenCalledWith(7, 'I have photos of the box'))
  })
})

describe('SellerReturnsPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('a new return can be approved', async () => {
    returnsApi.fetchSellerReturns.mockResolvedValue(page([ret('REQUESTED')]))
    returnsApi.approveReturn.mockResolvedValue(ret('APPROVED'))
    const user = renderWith(<SellerReturnsPage />)
    await user.click(await screen.findByRole('button', { name: 'Approve' }))
    expect(returnsApi.approveReturn).toHaveBeenCalledWith(7)
  })

  it('"Picked up" asks first, then refunds', async () => {
    returnsApi.fetchSellerReturns.mockResolvedValue(page([ret('APPROVED')]))
    returnsApi.markReturnPickedUp.mockResolvedValue(ret('REFUNDED'))
    const user = renderWith(<SellerReturnsPage />)
    await user.click(await screen.findByRole('button', { name: 'Picked up' }))
    expect(returnsApi.markReturnPickedUp).not.toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: 'Yes - refund the customer' }))
    await waitFor(() => expect(returnsApi.markReturnPickedUp).toHaveBeenCalledWith(7))
  })
})

describe('AdminReturnsPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('a dispute can be approved', async () => {
    returnsApi.fetchAdminReturns.mockResolvedValue(page([ret('ESCALATED', { rejectReason: 'Worn outdoors' })]))
    returnsApi.adminApproveReturn.mockResolvedValue(ret('APPROVED'))
    const user = renderWith(<AdminReturnsPage />)
    expect(await screen.findByText(/Worn outdoors/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Approve return' }))
    expect(returnsApi.adminApproveReturn).toHaveBeenCalledWith(7)
  })

  it('a failed refund can be retried', async () => {
    returnsApi.fetchAdminReturns.mockImplementation((status) =>
      Promise.resolve(page(status === 'REFUND_FAILED' ? [ret('REFUND_FAILED')] : [])))
    returnsApi.retryReturnRefund.mockResolvedValue(ret('REFUNDED'))
    const user = renderWith(<AdminReturnsPage />)
    await user.click(await screen.findByRole('tab', { name: 'Failed refunds' }))
    await user.click(await screen.findByRole('button', { name: 'Retry refund' }))
    expect(returnsApi.retryReturnRefund).toHaveBeenCalledWith(7)
  })
})
