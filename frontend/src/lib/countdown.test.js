import { describe, expect, it } from 'vitest'

import { clockOffset, formatClock, secondsLeft } from './countdown'

describe('countdown', () => {
  it('counts against the server clock even when the phone clock is wrong', () => {
    const phoneNow = Date.parse('2026-10-15T10:00:00') // the phone thinks it is 10:00
    const offset = clockOffset('2026-10-15T12:00:00', phoneNow) // the server says 12:00
    expect(secondsLeft('2026-10-15T12:10:00', offset, phoneNow)).toBe(600)
    expect(secondsLeft('2026-10-15T12:10:00', offset, phoneNow + 5_000)).toBe(595)
  })

  it('never goes below zero', () => {
    expect(secondsLeft('2026-10-15T12:00:00', 0, Date.parse('2026-10-15T13:00:00'))).toBe(0)
  })

  it('shows m:ss under an hour and h:mm:ss above', () => {
    expect(formatClock(125)).toBe('2:05')
    expect(formatClock(3725)).toBe('1:02:05')
    expect(formatClock(0)).toBe('0:00')
    expect(formatClock(-4)).toBe('0:00')
  })
})
