import { defineConfig, devices } from '@playwright/test'

import { DB, DEMO_PASSWORD } from './settings.js'

/**
 * Phase 11 end-to-end test: a REAL browser (Chromium) clicks through the real React app, which calls the real
 * Spring Boot jar, which uses a real MySQL. Nothing is faked except the payment page (no Stripe key = test page).
 *
 *   npm run db:up   -> throwaway MySQL on 3308       (CI: a "services:" container instead)
 *   npm test        -> starts the backend jar + the Vite dev server, then runs tests/*.spec.js
 *   npm run db:down -> removes the database again
 *
 * The backend jar must be built first: backend> mvnw -DskipTests package
 */
export default defineConfig({
  testDir: './tests',
  timeout: 180_000, // one test = the whole demo story (two people, two sellers, a refund)
  expect: { timeout: 15_000 },
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:5173',
    ...devices['Desktop Chrome'],
    trace: 'retain-on-failure', // on a failure: a step-by-step recording to open with "npx playwright show-trace"
    screenshot: 'only-on-failure',
  },
  webServer: [
    {
      command: 'java -jar ../backend/target/trivoko-backend-0.0.1-SNAPSHOT.jar',
      url: 'http://localhost:8080/api/health', // answers only after the search index is built (SearchIndexer)
      timeout: 180_000,
      reuseExistingServer: false,
      stdout: 'ignore',
      env: {
        SPRING_PROFILES_ACTIVE: 'dev',
        // environment variables beat ../.env, so the developer's own database and password are never used
        SPRING_DATASOURCE_URL: `jdbc:mysql://localhost:${DB.port}/${DB.name}`,
        MYSQL_USER: DB.user,
        MYSQL_PASSWORD: DB.password,
        DEMO_PASSWORD,
        MAIL_ENABLED: 'false',
        STRIPE_SECRET_KEY: '', // always the built-in test payment page
        SEARCH_INDEX_DIR: './test-results/search-index',
      },
    },
    {
      command: 'npm --prefix ../frontend run dev -- --port 5173 --strictPort',
      url: 'http://localhost:5173',
      timeout: 60_000,
      reuseExistingServer: false,
    },
  ],
})
