import { describe, expect, it } from 'vitest'

import { DEFAULT_FILTERS, parseFilters, toApiParams, toSearchParams } from './productFilters'

const parse = (query) => parseFilters(new URLSearchParams(query))

describe('parseFilters', () => {
  it('reads every filter from the URL', () => {
    expect(parse('q=case&category=phones&brand=Volta&brand=Arc&minPrice=100&maxPrice=2000&inStock=true&sort=price&dir=desc&page=2'))
      .toEqual({ q: 'case', category: 'phones', brands: ['Volta', 'Arc'], minPrice: 100, maxPrice: 2000,
        inStock: true, sort: 'price', dir: 'desc', page: 2 })
  })

  it('an empty URL gives the defaults', () => {
    expect(parse('')).toEqual(DEFAULT_FILTERS)
  })

  // Review focus: a hand-edited or old link must never break the page
  it('parseFilters drops bad values', () => {
    expect(parse('page=-3&minPrice=abc&maxPrice=-5&sort=xyz&dir=up&inStock=maybe&q=%20%20'))
      .toEqual(DEFAULT_FILTERS)
    expect(parse('page=2.5').page).toBe(1)
  })
})

describe('toSearchParams', () => {
  it('leaves out the defaults so links stay short', () => {
    expect(toSearchParams({ ...DEFAULT_FILTERS, category: 'phones' }).toString()).toBe('category=phones')
    expect(toSearchParams(DEFAULT_FILTERS).toString()).toBe('')
  })

  it('round-trips a full set of filters', () => {
    const filters = { q: 'run shoes', category: 'footwear', brands: ['Kovai Run', 'Stride'], minPrice: 500,
      maxPrice: 3000, inStock: true, sort: 'price', dir: 'asc', page: 3 }
    expect(parseFilters(toSearchParams(filters))).toEqual(filters)
  })
})

describe('toApiParams', () => {
  it('turns page 2 (people count from 1) into API page 1, and repeats brand', () => {
    const p = toApiParams({ ...DEFAULT_FILTERS, brands: ['Volta', 'Arc'], page: 2, inStock: true })
    expect(p.get('page')).toBe('1')
    expect(p.getAll('brand')).toEqual(['Volta', 'Arc'])
    expect(p.get('inStock')).toBe('true')
    expect(p.get('size')).toBe('24')
  })
})
