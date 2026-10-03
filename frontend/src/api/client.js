import axios from 'axios'

// All backend calls go through this one "messenger".
// "/api" is forwarded to Spring Boot (localhost:8080) by vite.config.js.
// Errors: every page turns a failed call into a friendly message with describeError() (hooks/useAsync.js),
// which reads the backend's problem-details JSON ({ detail, errors }).
const api = axios.create({
  baseURL: '/api',
  timeout: 10_000, // give up after 10 seconds instead of spinning forever
})

export default api
