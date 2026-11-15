import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchCategories, fetchProduct } from '../api/catalog'
import CompareButton from '../components/catalog/CompareButton'
import { compareList } from '../lib/compareList'
import ComparePage from './ComparePage'

vi.mock('../api/catalog')

/** After launch: pick two products, compare them side by side. */

const product = (over) => ({
  id: 1, slug: 'volta-v12-5g', name: 'Volta V12 5G', brand: 'Volta', description: 'A phone.',
  priceFrom: 12999, mrpFrom: 14499, discountPercent: 10, category: { name: 'Mobile phones', slug: 'phones' },
  seller: { shopName: 'Chennai Mobiles', slug: 'chennai-mobiles', city: 'Chennai' },
  variants: [
    { id: 1, colour: 'Black', size: '128 GB', inStock: true },
    { id: 2, colour: 'Blue', size: '256 GB', inStock: false },
  ],
  images: [], ratingAvg: 4.5, ratingCount: 2, lowestPrice30Days: 12499, lowestNow: false,
  ...over,
})

const volta = product()
const kaveri = product({ id: 2, slug: 'kaveri-k5', name: 'Kaveri K5', brand: 'Kaveri', priceFrom: 8999, ratingCount: 0, variants: [{ id: 3, colour: 'Grey', size: '64 GB', inStock: true }] })

function renderAt(url, ui = <ComparePage />) {
  render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path="/compare" element={ui} />
        <Route path="/products/:slug" element={ui} />
      </Routes>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

describe('ComparePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    vi.mocked(fetchCategories).mockResolvedValue([]) // ProductImage looks up the category's colour
  })

  it('shows both products side by side and marks the cheaper one', async () => {
    vi.mocked(fetchProduct).mockImplementation((slug) => Promise.resolve(slug === 'kaveri-k5' ? kaveri : volta))
    renderAt('/compare?p=volta-v12-5g,kaveri-k5')

    const table = await screen.findByRole('table')
    expect(within(table).getByText('Volta V12 5G')).toBeInTheDocument()
    expect(within(table).getByText('Kaveri K5')).toBeInTheDocument()
    expect(within(table).getAllByText('Cheaper')).toHaveLength(1)
    const stockRow = within(table).getByRole('row', { name: /In stock/ })
    expect(stockRow).toHaveTextContent('1 of 2 options')
    expect(within(table).getByRole('row', { name: /Rating/ })).toHaveTextContent('No reviews yet')
  })

  it('leaves out a product that is no longer on sale', async () => {
    vi.mocked(fetchProduct).mockImplementation((slug) => (slug === 'kaveri-k5' ? Promise.resolve(kaveri) : Promise.reject(new Error('404'))))
    renderAt('/compare?p=volta-v12-5g,kaveri-k5')
    expect(await screen.findByText('Kaveri K5')).toBeInTheDocument()
    expect(screen.queryByText('Volta V12 5G')).not.toBeInTheDocument()
    expect(screen.getByText(/Pick one more product/)).toBeInTheDocument()
  })

  it('says what to do when nothing is picked', () => {
    renderAt('/compare')
    expect(screen.getByText('Nothing to compare yet')).toBeInTheDocument()
    expect(fetchProduct).not.toHaveBeenCalled()
  })

  it('the Compare tick on two product pages offers "Compare now"', async () => {
    compareList.toggle('kaveri-k5')
    const user = renderAt('/products/volta-v12-5g', <CompareButton slug="volta-v12-5g" />)
    await user.click(screen.getByRole('checkbox', { name: 'Compare' }))
    expect(screen.getByRole('link', { name: /Compare now/ })).toHaveAttribute('href', '/compare?p=kaveri-k5,volta-v12-5g')
  })
})
