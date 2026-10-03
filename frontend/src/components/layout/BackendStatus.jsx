import { useEffect, useState } from 'react'

import api from '../../api/client'

const dots = {
  UP: 'bg-green-500',
  DOWN: 'bg-red-500',
  checking: 'bg-amber-400',
}

/** Small "Backend: UP" light, shown in the footer. */
export default function BackendStatus() {
  const [status, setStatus] = useState('checking')

  useEffect(() => {
    api
      .get('/health')
      .then((res) => setStatus(res.data.status))
      .catch(() => setStatus('DOWN'))
  }, [])

  return (
    <span className="inline-flex items-center gap-2" title="Is the Spring Boot backend running?">
      <span className={`h-2 w-2 rounded-full ${dots[status] ?? dots.DOWN}`} />
      Backend: {status}
    </span>
  )
}
