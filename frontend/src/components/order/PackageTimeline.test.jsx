import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import PackageTimeline from './PackageTimeline'

const ev = (status, note, at) => ({ status, note, at })

describe('PackageTimeline', () => {
  it('shows the steps done with their time, and the next steps still to come', () => {
    render(<PackageTimeline status="SHIPPED" trackingNumber="AWB12345" events={[
      ev('PLACED', 'Payment received', '2026-10-05T10:42:00'),
      ev('PACKED', 'Packed by Kovai Sports', '2026-10-05T12:00:00'),
      ev('SHIPPED', 'Tracking number AWB12345', '2026-10-05T15:10:00'),
    ]} />)
    const steps = screen.getAllByRole('listitem')
    expect(steps.map((li) => li.dataset.state)).toEqual(['done', 'done', 'done', 'next'])
    expect(screen.getByText('Shipped')).toBeInTheDocument()
    expect(screen.getByText(/AWB12345/)).toBeInTheDocument()
    expect(screen.getByText('Delivered')).toBeInTheDocument()
  })

  it('a cancelled package ends with "Cancelled" and its reason', () => {
    render(<PackageTimeline status="CANCELLED" events={[
      ev('PLACED', 'Payment received', '2026-10-05T10:42:00'),
      ev('CANCELLED', 'Size UK 8 is damaged', '2026-10-05T11:00:00'),
    ]} />)
    expect(screen.getByText('Cancelled')).toBeInTheDocument()
    expect(screen.getByText(/Size UK 8 is damaged/)).toBeInTheDocument()
    expect(screen.queryByText('Delivered')).toBeNull() // no future steps after a cancel
  })
})
