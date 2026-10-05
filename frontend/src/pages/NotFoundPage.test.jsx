import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'

import api from '../api/client'
import NotFoundPage from './NotFoundPage'

const TREE = [
  { id: 1, name: 'Electronics', slug: 'electronics', children: [] },
  { id: 2, name: 'Fashion', slug: 'fashion', children: [] },
  { id: 3, name: 'Home & Kitchen', slug: 'home-kitchen', children: [] },
  { id: 4, name: 'Books', slug: 'books', children: [] },
  { id: 5, name: 'Sports', slug: 'sports', children: [] },
  { id: 6, name: 'Kids', slug: 'kids', children: [] },
  { id: 7, name: 'Grocery', slug: 'grocery', children: [] },
]

/** Shows where the app went, so a test can check the search box's target URL. */
function WhereAmI() {
  const location = useLocation()
  return <p>At {location.pathname + location.search}</p>
}

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/products" element={<WhereAmI />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('NotFoundPage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('says the shelf is empty and sets the tab title', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({ data: TREE })
    renderAt('/no-such-page')
    expect(screen.getByRole('heading', { level: 1, name: 'This shelf is empty' })).toBeInTheDocument()
    expect(document.title).toBe('Page not found - TriVoKo')
    await screen.findByText('Popular:')
  })

  it('shows at most 6 real categories as links to the product list', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({ data: TREE })
    renderAt('/no-such-page')
    const electronics = await screen.findByRole('link', { name: 'Electronics' })
    expect(electronics).toHaveAttribute('href', '/products?category=electronics')
    expect(screen.getByRole('link', { name: 'Kids' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Grocery' })).not.toBeInTheDocument()
    expect(api.get).toHaveBeenCalledWith('/categories')
  })

  it('still works when the categories cannot be loaded', async () => {
    vi.spyOn(api, 'get').mockRejectedValue(new Error('Network Error'))
    renderAt('/no-such-page')
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument()
    // give the failed request a moment, then the "Popular" row must not appear
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(screen.queryByText('Popular:')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Go home' })).toHaveAttribute('href', '/')
  })

  it('searches the shop', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({ data: TREE })
    const user = userEvent.setup()
    renderAt('/no-such-page')
    await user.type(screen.getByRole('searchbox', { name: 'Search products' }), 'running shoes')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    expect(await screen.findByText('At /products?q=running+shoes')).toBeInTheDocument()
  })

  it('does not search for an empty text', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({ data: TREE })
    const user = userEvent.setup()
    renderAt('/no-such-page')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    expect(screen.getByRole('heading', { level: 1, name: 'This shelf is empty' })).toBeInTheDocument()
  })
})
