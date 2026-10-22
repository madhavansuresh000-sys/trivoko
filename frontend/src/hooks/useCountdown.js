import { useEffect, useMemo, useState } from 'react'

import { clockOffset, secondsLeft } from '../lib/countdown'

/**
 * Seconds left until `targetIso` (a server time), ticking every second.
 * `serverNowIso` is the server's clock when the answer arrived; it fixes the phone-vs-server difference.
 *   const left = useCountdown(sale.endsAt, sale.serverNow)
 */
export default function useCountdown(targetIso, serverNowIso) {
  const offset = useMemo(() => (serverNowIso ? clockOffset(serverNowIso) : 0), [serverNowIso])
  const [left, setLeft] = useState(() => (targetIso ? secondsLeft(targetIso, offset) : 0))

  useEffect(() => {
    if (!targetIso) return undefined
    const tick = () => setLeft(secondsLeft(targetIso, offset))
    tick()
    const timer = setInterval(tick, 1000)
    return () => clearInterval(timer)
  }, [targetIso, offset])

  return left
}
