import { configureStore } from '@reduxjs/toolkit'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as catalog from '../api/catalog'
import * as searchApi from '../api/search'
import AlsoBought from '../components/catalog/AlsoBought'
import SearchBox from '../components/search/SearchBox'
import authReducer from '../store/authSlice'
import notificationsReducer from '../store/notificationsSlice'
import wishlistReducer from '../store/wishlistSlice'
import ProductsPage from './ProductsPage'

vi.mock('../api/search')
vi.mock('../api/catalog')

/** Phase 10 step 5-6 - suggestions while typing, the search results with their counts, "also bought". */

/** Shows where the app is, so a test can check what a click or Enter opened. */
function Where() {
  const location = useLocation()
  return <p data-testid="where">{location.pathname + location.search}</p>
}

function renderAt(url, ui) {
  const store = configureStore({ reducer: { auth: authReducer, wishlist: wishlistReducer, notifications: notificationsReducer } })
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[url]}>
        <Routes>
          <Route path="*" element={<>{ui}<Where /></>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  )
  return userEvent.setup()
}

const where = () => screen.getByTestId('where').textContent

const card = (id, slug, name, brand = 'Volta') => ({
  id, slug, name, brand, priceFrom: 12999, mrpFrom: 14499, discountPercent: 10, imageUrl: null, inStock: true,
  sellerName: 'Chennai Mobiles', sellerSlug: 'chennai-mobiles', categorySlug: 'phones', ratingAvg: null, ratingCount: 0,
})

const page = (content) => ({ content, page: 0, size: 24, totalElements: content.length, totalPages: 1, first: true, last: true })

describe('SearchBox', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    searchApi.fetchSuggestions.mockResolvedValue([
      { type: 'brand', text: 'Volta', slug: null },
      { type: 'product', text: 'Volta V12 5G', slug: 'volta-v12-5g' },
    ])
  })

  it('asks for suggestions only from 2 letters, and only once the typing pauses', async () => {
    const user = renderAt('/', <SearchBox />)
    const box = screen.getByRole('combobox', { name: 'Search products' })

    await user.type(box, 'v')
    await new Promise((r) => setTimeout(r, 350))
    expect(searchApi.fetchSuggestions).not.toHaveBeenCalled()

    await user.type(box, 'olt') // 3 quick keys after "v" -> ONE request for "volt"
    await waitFor(() => expect(searchApi.fetchSuggestions).toHaveBeenCalledTimes(1))
    expect(searchApi.fetchSuggestions).toHaveBeenCalledWith('volt')
    expect(await screen.findByRole('option', { name: /Volta V12 5G/ })).toBeInTheDocument()
    expect(box).toHaveAttribute('aria-expanded', 'true')
  })

  it('arrow keys pick a suggestion and Enter opens it', async () => {
    const user = renderAt('/', <SearchBox />)
    await user.type(screen.getByRole('combobox'), 'vo')
    await screen.findByRole('option', { name: /Volta V12 5G/ })

    await user.keyboard('{ArrowDown}{ArrowDown}')
    expect(screen.getByRole('option', { name: /Volta V12 5G/ })).toHaveAttribute('aria-selected', 'true')
    await user.keyboard('{Enter}')
    expect(where()).toBe('/products/volta-v12-5g')
  })

  it('a brand suggestion shows that brand, Escape closes the list, Enter alone searches the words', async () => {
    const user = renderAt('/', <SearchBox />)
    const box = screen.getByRole('combobox')
    await user.type(box, 'vo')
    await user.click(await screen.findByRole('option', { name: /Brand/ }))
    expect(where()).toBe('/products?brand=Volta')

    await user.clear(box)
    await user.type(box, 'iphnoe')
    await screen.findByRole('option', { name: /Volta V12 5G/ })
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    await user.keyboard('{Enter}')
    expect(where()).toBe('/products?q=iphnoe')
  })
})

describe('Search results page', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    catalog.fetchBrands.mockResolvedValue([])
    catalog.fetchCategories.mockResolvedValue([])
    searchApi.searchProducts.mockResolvedValue({
      results: page([card(5, 'silicone-case-for-iphone-15', 'Silicone case for iPhone 15', 'ShieldUp')]),
      facets: {
        brands: [{ value: 'ShieldUp', count: 1 }, { value: 'Volta', count: 2 }],
        categories: [{ slug: 'cases-covers', name: 'Cases & covers', count: 3 }],
        prices: [{ label: 'Under ₹500', min: null, max: 499.99, count: 3 }, { label: '₹10,000 and above', min: 10000, max: null, count: 0 }],
      },
    })
  })

  const lastParams = () => Object.fromEntries(searchApi.searchProducts.mock.calls.at(-1)[0])

  it('words go to the smart search, best match first, with the counts of THIS search', async () => {
    renderAt('/products?q=iphnoe', <ProductsPage />)
    expect(await screen.findByText('Silicone case for iPhone 15')).toBeInTheDocument()
    expect(catalog.fetchProducts).not.toHaveBeenCalled()
    expect(lastParams()).toMatchObject({ q: 'iphnoe', page: '0' })
    expect(screen.getAllByRole('combobox', { name: 'Sort by' })[0]).toHaveDisplayValue('Best match')
    expect(screen.getAllByText('Cases & covers')[0]).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /₹10,000 and above/ })[0]).toBeDisabled() // 0 results there
  })

  it('a category or price band narrows the search and keeps the words', async () => {
    const user = renderAt('/products?q=iphnoe', <ProductsPage />)
    await screen.findByText('Silicone case for iPhone 15')

    await user.click(screen.getAllByRole('button', { name: /Cases & covers/ })[0])
    await waitFor(() => expect(where()).toBe('/products?q=iphnoe&category=cases-covers'))
    await user.click(screen.getAllByRole('button', { name: /Under ₹500/ })[0])
    await waitFor(() => expect(lastParams()).toMatchObject({ q: 'iphnoe', category: 'cases-covers', maxPrice: '499.99' }))
  })
})

describe('AlsoBought', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows two or more products', async () => {
    searchApi.fetchAlsoBought.mockResolvedValue([card(2, 'a', 'Arc Pulse ANC headphones', 'Arc'), card(3, 'b', 'Volta 33W fast charger')])
    renderAt('/products/volta-v12-5g', <AlsoBought slug="volta-v12-5g" />)
    expect(await screen.findByText('Customers also bought')).toBeInTheDocument()
    expect(screen.getByText('Volta 33W fast charger')).toBeInTheDocument()
  })

  it('stays hidden with just one (no lonely card, no random fillers)', async () => {
    searchApi.fetchAlsoBought.mockResolvedValue([card(2, 'a', 'Arc Pulse ANC headphones', 'Arc')])
    renderAt('/products/volta-v12-5g', <AlsoBought slug="volta-v12-5g" />)
    await waitFor(() => expect(searchApi.fetchAlsoBought).toHaveBeenCalled())
    expect(screen.queryByText('Customers also bought')).not.toBeInTheDocument()
  })
})
