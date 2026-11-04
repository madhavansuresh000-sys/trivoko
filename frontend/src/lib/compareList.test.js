import { beforeEach, describe, expect, it } from 'vitest'

import { COMPARE_KEY, compareLink, compareList, slugsFromQuery } from './compareList'

describe('compareList', () => {
  beforeEach(() => localStorage.clear())

  it('keeps two products; a third pushes out the oldest', () => {
    compareList.toggle('volta-v12-5g')
    compareList.toggle('kaveri-k5')
    compareList.toggle('nimbus-n8-pro')
    expect(compareList.read()).toEqual(['kaveri-k5', 'nimbus-n8-pro'])
  })

  it('a second click on the same product removes it', () => {
    compareList.toggle('kaveri-k5')
    compareList.toggle('kaveri-k5')
    expect(compareList.has('kaveri-k5')).toBe(false)
  })

  it('ignores junk in storage and in the URL', () => {
    localStorage.setItem(COMPARE_KEY, '["<script>", "kaveri-k5", 7]')
    expect(compareList.read()).toEqual(['kaveri-k5'])
    expect(slugsFromQuery('a-b,../x,c-d,e-f')).toEqual(['a-b', 'c-d'])
    expect(slugsFromQuery(null)).toEqual([])
  })

  it('builds a shareable link', () => {
    expect(compareLink(['a-b', 'c-d'])).toBe('/compare?p=a-b,c-d')
  })
})
