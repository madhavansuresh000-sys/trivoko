import { configureStore } from '@reduxjs/toolkit'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as catalog from '../../api/catalog'
import * as seller from '../../api/seller'
import notificationsReducer from '../../store/notificationsSlice'
import ProductEditorPage from './ProductEditorPage'

vi.mock('../../api/seller')
vi.mock('../../api/catalog')

const tree = [{ id: 21, name: 'Footwear', slug: 'footwear', children: [{ id: 22, name: 'Running shoes', slug: 'running-shoes', children: [] }] }]

const live = { id: 61, name: 'Kovai Run Swift', slug: 'kovai-run-swift', brand: 'Kovai Run', description: 'Light shoes',
  categoryId: 22, categoryName: 'Running shoes', status: 'ACTIVE', priceFrom: 2499, mrpFrom: 3399, images: [],
  variants: [{ id: 157, sku: 'P61-A', label: 'UK 7', size: '7', colour: null, price: 2499, mrp: 3399, stock: 12 }] }

function renderAt(url) {
  const store = configureStore({ reducer: { notifications: notificationsReducer } })
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[url]}>
        <Routes>
          <Route path="/seller/products/new" element={<ProductEditorPage />} />
          <Route path="/seller/products/:id" element={<ProductEditorPage />} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  )
  return userEvent.setup()
}

describe('ProductEditorPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    catalog.fetchCategories.mockResolvedValue(tree)
  })

  it('a new product starts with one variant row; rows can be added and removed', async () => {
    const user = renderAt('/seller/products/new')
    expect(await screen.findAllByLabelText(/Variant label/)).toHaveLength(1)
    await user.click(screen.getByRole('button', { name: /Add a variant/ }))
    expect(screen.getAllByLabelText(/Variant label/)).toHaveLength(2)
    await user.click(screen.getAllByRole('button', { name: /Remove variant/ })[1])
    expect(screen.getAllByLabelText(/Variant label/)).toHaveLength(1)
    expect(screen.queryByText(/Photos/)).toBeNull() // photos come after the first save
  })

  it('a live product locks its name and labels; price and stock stay editable', async () => {
    seller.fetchMyProduct.mockResolvedValue(live)
    renderAt('/seller/products/61')
    expect(await screen.findByDisplayValue('Kovai Run Swift')).toBeDisabled()
    expect(screen.getByDisplayValue('UK 7')).toBeDisabled()
    expect(screen.getByDisplayValue('2499')).toBeEnabled()
    expect(screen.getByDisplayValue('12')).toBeEnabled()
    expect(screen.getByText(/live product can only change its prices and stock/i)).toBeInTheDocument()
  })
})
