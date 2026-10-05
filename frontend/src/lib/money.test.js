import { describe, expect, it } from 'vitest'

import { formatRupees } from './money'

describe('formatRupees', () => {
  it('uses Indian grouping and no .00 for whole rupees', () => {
    expect(formatRupees(2499)).toBe('₹2,499')
    expect(formatRupees('149999.00')).toBe('₹1,49,999')
  })

  it('keeps paise when there are some', () => {
    expect(formatRupees(99.5)).toBe('₹99.50')
  })

  it('shows a dash for a missing price', () => {
    expect(formatRupees(null)).toBe('–')
    expect(formatRupees(undefined)).toBe('–')
  })
})
