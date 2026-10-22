// The countdown counts against the SERVER's clock, not the phone's: offset = server time - this phone's time,
// measured once when the answer arrives. (Both server times are "local" strings, so they cancel out.)

const parse = (iso) => new Date(iso).getTime()

/** How far the server's clock is ahead of this phone (milliseconds, can be negative). */
export const clockOffset = (serverNowIso, phoneNowMs = Date.now()) => parse(serverNowIso) - phoneNowMs

/** Whole seconds until targetIso on the server's clock; never below 0. */
export const secondsLeft = (targetIso, offsetMs, phoneNowMs = Date.now()) =>
  Math.max(0, Math.floor((parse(targetIso) - (phoneNowMs + offsetMs)) / 1000))

/** 3725 -> "1:02:05", 125 -> "2:05" */
export function formatClock(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = String(s % 60).padStart(2, '0')
  return h > 0 ? `${h}:${String(m).padStart(2, '0')}:${sec}` : `${m}:${sec}`
}
