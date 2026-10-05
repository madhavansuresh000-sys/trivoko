import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as catalog from '../api/catalog'
import ProductsPage from './ProductsPage'

vi.mock('../api/catalog')

const page = (content) => ({ content, page: 0, size: 24, totalElements: content.length, totalPages: 1, first: true, last: true })
const card = { id: 1, slug: 'volta-v12-5g', name: 'Volta V12 5G', brand: 'Volta', priceFrom: 12999, mrpFrom: 14499,
  discountPercent: 10, imageUrl: null, inStock: true, sellerName: 'Chennai Mobiles', sellerSlug: 'chennai-mobiles', categorySlug: 'phones' }

function renderAt(url) {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Routes><Route path="/products" element={<ProductsPage />} /></Routes>
    </MemoryRouter>,
  )
}

/** The params of the n-th GET /api/products call, as a plain object (brand can repeat). */
const call = (n) => {
  const p = catalog.fetchProducts.mock.calls[n][0]
  return { category: p.get('category'), brand: p.getAll('brand'), page: p.get('page'), inStock: p.get('inStock') }
}

describe('ProductsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    catalog.fetchProducts.mockResolvedValue(page([card]))
    catalog.fetchBrands.mockResolvedValue([{ brand: 'Volta', count: 1 }, { brand: 'Kaveri', count: 1 }])
    catalog.fetchCategories.mockResolvedValue([])
  })

  it('reads the filters from the URL (page 2 in the URL = API page 1)', async () => {
    renderAt('/products?category=phones&brand=Volta&page=2')
    expect(await screen.findByText('Volta V12 5G')).toBeInTheDocument()
    expect(call(0)).toEqual({ category: 'phones', brand: ['Volta'], page: '1', inStock: null })
    expect(catalog.fetchBrands).toHaveBeenCalledWith('phones')
  })

  it('ticking "In stock only" puts it in the URL and goes back to page 1', async () => {
    renderAt('/products?category=phones&page=3')
    await screen.findByText('Volta V12 5G')
    await userEvent.click(screen.getAllByLabelText('In stock only')[0])
    await waitFor(() => expect(catalog.fetchProducts).toHaveBeenCalledTimes(2))
    expect(call(1)).toEqual({ category: 'phones', brand: [], page: '0', inStock: 'true' })
  })

  it('shows a friendly empty state with "Clear filters"', async () => {
    catalog.fetchProducts.mockResolvedValue(page([]))
    renderAt('/products?q=zzzz')
    expect(await screen.findByText('No products match')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Clear filters' })).toHaveAttribute('href', '/products')
  })
})
