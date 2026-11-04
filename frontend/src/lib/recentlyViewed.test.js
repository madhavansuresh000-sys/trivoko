import { beforeEach, describe, expect, it } from 'vitest'

import { MAX_RECENT, RECENT_KEY, recentlyViewed } from './recentlyViewed'

describe('recentlyViewed', () => {
  beforeEach(() => localStorage.clear())

  it('keeps the newest first and each product once', () => {
    recentlyViewed.add(3)
    recentlyViewed.add(17)
    recentlyViewed.add(3)
    expect(recentlyViewed.read()).toEqual([3, 17])
  })

  it('forgets the oldest after 12', () => {
    for (let id = 1; id <= MAX_RECENT + 3; id++) recentlyViewed.add(id)
    const ids = recentlyViewed.read()
    expect(ids).toHaveLength(MAX_RECENT)
    expect(ids[0]).toBe(MAX_RECENT + 3)
    expect(ids).not.toContain(1)
  })

  it('ignores junk instead of crashing the shop', () => {
    localStorage.setItem(RECENT_KEY, '{"not":"a list"}')
    expect(recentlyViewed.read()).toEqual([])
    localStorage.setItem(RECENT_KEY, '[5, "x", -1, 2.5, 8]')
    expect(recentlyViewed.read()).toEqual([5, 8])
  })
})
