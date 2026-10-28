import { expect, test } from '@playwright/test'

import { DEMO_PASSWORD } from '../settings.js'

/**
 * The TriVoKo demo story as one automated walk-through (Phase 11 step 1):
 *
 *   Ravi buys a phone from Chennai Mobiles + shoes from Kovai Sports   -> ONE payment, TWO packages
 *   Kovai Sports packs, ships, delivers its package
 *   Ravi returns the shoes -> Kovai Sports approves and collects them -> Ravi is refunded
 *
 * Two browser "contexts" = two people on two different computers (each has its own login cookie).
 */

const PHONE = 'volta-v12-5g' // Chennai Mobiles
const SHOES = 'kovai-run-swift-running-shoes' // Kovai Sports

async function logIn(browser, who) {
  const context = await browser.newContext()
  const page = await context.newPage()
  await page.goto('/login')
  await page.getByLabel('Email').fill(`${who}@trivoko.test`)
  await page.getByLabel('Password', { exact: true }).fill(DEMO_PASSWORD)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page.getByRole('button', { name: 'Account menu' })).toBeVisible()
  return page
}

/** Waits for the small pop-up message at the bottom ("Address saved", "Return requested ..."). */
const toast = (page, text) => expect(page.getByText(text).first()).toBeVisible()

test('a customer buys from two sellers, one seller ships, the customer returns and is refunded', async ({ browser }) => {
  const ravi = await logIn(browser, 'ravi')

  await test.step('Ravi adds a delivery address', async () => {
    await ravi.goto('/account/addresses')
    await ravi.getByRole('button', { name: 'Add your first address' }).click()
    await ravi.getByLabel('Full name').fill('Ravi Kumar')
    await ravi.getByLabel('Mobile number').fill('9876543210')
    await ravi.getByLabel('House no., building, street').fill('12 Anna Salai')
    await ravi.getByLabel('City').fill('Chennai')
    await ravi.getByLabel('State').selectOption('Tamil Nadu')
    await ravi.getByLabel('Pincode').fill('600002')
    await ravi.getByRole('button', { name: 'Save address' }).click()
    await toast(ravi, 'Address saved')
  })

  await test.step('Ravi puts a phone and shoes from two different shops in one cart', async () => {
    for (const slug of [PHONE, SHOES]) {
      await ravi.goto(`/products/${slug}`)
      await ravi.getByRole('button', { name: 'Add to cart' }).click()
      await toast(ravi, 'Added to cart')
    }
    await ravi.goto('/cart')
    await expect(ravi.getByRole('region', { name: /Package 1 from/ })).toBeVisible()
    await expect(ravi.getByRole('region', { name: /Package 2 from/ })).toBeVisible()
  })

  let orderNumber
  await test.step('Ravi pays once for both shops (built-in test payment page)', async () => {
    await ravi.getByRole('link', { name: 'Checkout' }).click()
    await ravi.getByRole('button', { name: /^Pay ₹/ }).click()
    await expect(ravi).toHaveURL(/\/test-payment\//)
    await ravi.getByRole('button', { name: /^Pay ₹/ }).click()
    const heading = ravi.getByRole('heading', { name: /^Order TV-\d+ placed!$/ })
    await expect(heading).toBeVisible()
    orderNumber = (await heading.textContent()).match(/TV-\d+/)[0]
  })

  const kovai = await logIn(browser, 'kovai.sports')
  await test.step('Kovai Sports packs, ships and delivers its package', async () => {
    await kovai.goto('/seller/orders')
    await expect(kovai.getByText(orderNumber)).toBeVisible()
    await kovai.getByRole('button', { name: 'Mark packed' }).click()
    await toast(kovai, `Package ${orderNumber} packed`)

    await kovai.getByRole('tab', { name: 'To ship' }).click()
    await kovai.getByRole('button', { name: 'Ship', exact: true }).click()
    await kovai.getByLabel('Tracking number (AWB)').fill('AWB12345678')
    await kovai.getByRole('button', { name: 'Mark shipped' }).click()
    await toast(kovai, `Package ${orderNumber} shipped`)

    await kovai.getByRole('tab', { name: 'Shipped' }).click()
    await kovai.getByRole('button', { name: 'Mark delivered' }).click()
    await toast(kovai, `Package ${orderNumber} delivered`)
  })

  await test.step('Ravi returns the delivered shoes', async () => {
    await ravi.goto(`/orders/${orderNumber}`)
    await ravi.getByRole('button', { name: 'Return', exact: true }).click()
    await ravi.getByLabel('Why are you returning it?').selectOption('DAMAGED')
    await ravi.getByLabel('Tell the seller what is wrong').fill('The sole came apart after one run')
    await ravi.getByRole('button', { name: 'Ask for a return' }).click()
    await toast(ravi, 'Return requested')
  })

  await test.step('Kovai Sports approves, collects the shoes, and the money goes back', async () => {
    await kovai.goto('/seller/returns')
    await kovai.getByRole('button', { name: 'Approve' }).click()
    await kovai.getByRole('tab', { name: 'To collect' }).click()
    await kovai.getByRole('button', { name: 'Picked up' }).click()
    await kovai.getByRole('button', { name: 'Yes - refund the customer' }).click()
    await kovai.getByRole('tab', { name: 'Refunded' }).click()
    await expect(kovai.getByText('Refunded').last()).toBeVisible()
  })

  await test.step('Ravi sees the refund', async () => {
    await ravi.goto('/account/returns')
    await expect(ravi.getByText('Refunded').first()).toBeVisible()
  })
})
