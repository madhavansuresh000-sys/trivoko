/**
 * Shared settings of the end-to-end test. The E2E run has its OWN throwaway MySQL (port 3308), so it never touches
 * the developer's data on 3307; the demo accounts get this password there (DevDataSeeder reads DEMO_PASSWORD).
 * It is a test-only password for a throwaway database - not a secret.
 */
export const DEMO_PASSWORD = process.env.E2E_DEMO_PASSWORD ?? 'E2e-Demo-Pass-1'

export const DB = { port: 3308, name: 'trivoko', user: 'e2e', password: 'e2e-only' }
