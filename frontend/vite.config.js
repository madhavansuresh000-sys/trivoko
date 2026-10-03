import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // send /api/... requests to Spring Boot during development
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  // Phase 8: `npm test` runs the component tests (Vitest + React Testing Library) in a fake browser (jsdom)
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
})
