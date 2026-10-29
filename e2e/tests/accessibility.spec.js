import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

import { DEMO_PASSWORD } from '../settings.js'

/**
 * Phase 11 step 4: every main page, checked by axe (the standard accessibility checker) against WCAG 2.1 A + AA -
 * in light AND dark mode, on a laptop AND a phone. "Serious" or "critical" problems fail the test (e.g. text too
 * pale to read, a button with no name for a screen reader, a form field without a label).
 */

const PUBLIC_PAGES = ['/', '/products', '/products?q=iphnoe', '/products/volta-v12-5g', '/cart', '/login', '/register', '/flash']
const CUSTOMER_PAGES = ['/orders', '/wishlist', '/account/addresses', '/account/returns', '/checkout']
const SELLER_PAGES = ['/seller', '/seller/orders', '/seller/returns']
const ADMIN_PAGES = ['/admin']

const LOOKS = [
  { name: 'light, laptop', theme: 'light', viewport: { width: 1280, height: 800 } },
  { name: 'dark, phone', theme: 'dark', viewport: { width: 390, height: 844 } },
]

async function logIn(page, who) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(`${who}@trivoko.test`)
  await page.getByLabel('Password', { exact: true }).fill(DEMO_PASSWORD)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page.getByRole('button', { name: /account menu|Open menu/ }).first()).toBeAttached()
}

/** Only what really stops people: "serious" and "critical". Each problem is listed with where it is. */
async function check(page, url) {
  await page.goto(url)
  await page.waitForLoadState('networkidle')
  const { violations } = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  return violations
    .filter((v) => v.impact === 'serious' || v.impact === 'critical')
    .map((v) => `${url}: ${v.id} (${v.impact}) - ${v.help} - e.g. ${v.nodes[0]?.target.join(' ')}`)
}

for (const look of LOOKS) {
  test.describe(look.name, () => {
    test.use({ viewport: look.viewport })

    test.beforeEach(async ({ page }) => {
      await page.addInitScript((theme) => localStorage.setItem('theme', theme), look.theme)
    })

    test('public pages', async ({ page }) => {
      const problems = []
      for (const url of PUBLIC_PAGES) problems.push(...await check(page, url))
      expect(problems, problems.join('\n')).toEqual([])
    })

    test('customer pages', async ({ page }) => {
      await logIn(page, 'ravi')
      const problems = []
      for (const url of CUSTOMER_PAGES) problems.push(...await check(page, url))
      expect(problems, problems.join('\n')).toEqual([])
    })

    test('seller and admin pages', async ({ page, browser }) => {
      await logIn(page, 'kovai.sports')
      const problems = []
      for (const url of SELLER_PAGES) problems.push(...await check(page, url))
      const admin = await (await browser.newContext({ viewport: look.viewport })).newPage()
      await admin.addInitScript((theme) => localStorage.setItem('theme', theme), look.theme)
      await logIn(admin, 'admin')
      for (const url of ADMIN_PAGES) problems.push(...await check(admin, url))
      expect(problems, problems.join('\n')).toEqual([])
    })
  })
}
