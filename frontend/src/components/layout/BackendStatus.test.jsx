import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import api from '../../api/client'
import BackendStatus from './BackendStatus'

describe('BackendStatus', () => {
  afterEach(() => vi.restoreAllMocks())

  it('shows UP when /api/health answers', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({ data: { status: 'UP' } })
    render(<BackendStatus />)
    expect(await screen.findByText('Backend: UP')).toBeInTheDocument()
    expect(api.get).toHaveBeenCalledWith('/health')
  })

  it('shows DOWN when the backend is not running', async () => {
    vi.spyOn(api, 'get').mockRejectedValue(new Error('Network Error'))
    render(<BackendStatus />)
    expect(await screen.findByText('Backend: DOWN')).toBeInTheDocument()
  })
})
