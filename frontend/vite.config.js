import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vitest/config'
import vercel from './vercel.json' with { type: 'json' }

// Phase 12: `npm run preview` sends the same security headers as the live site (vercel.json), so a
// Content-Security-Policy mistake shows up on the laptop first
const liveHeaders = Object.fromEntries(vercel.headers[0].headers.map((h) => [h.key, h.value]))
delete liveHeaders['Strict-Transport-Security'] // HTTPS only; preview is plain http
delete liveHeaders['Content-Security-Policy'] // replaced below without upgrade-insecure-requests (http)
liveHeaders['Content-Security-Policy'] = vercel.headers[0].headers
  .find((h) => h.key === 'Content-Security-Policy')
  .value.replace('; upgrade-insecure-requests', '')

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // send /api/... requests to Spring Boot during development
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  preview: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
    headers: liveHeaders,
  },
  // Phase 8: `npm test` runs the component tests (Vitest + React Testing Library) in a fake browser (jsdom)
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
})
