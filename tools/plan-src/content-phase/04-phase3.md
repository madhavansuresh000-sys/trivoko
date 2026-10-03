# Phase 3 - Shop frontend

**Time:** 1.5 weeks  ·  **Group:** Core  ·  **Folder:** `Phase_3_Shop_Frontend\`

**Goal:** A customer can browse, search the catalogue, choose a size or colour and fill a cart from many sellers - on laptop and phone, light and dark. No payment yet.

>> The shop doors open and people can walk around and fill their trolleys. The billing counter still has a "coming soon" board.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Layout: navbar (search box, category menu, cart count, account menu), footer, mobile menu, Toaster | Claude |
| 2 | Home: deals, category tiles, new arrivals, flash-sale banner place | Claude |
| 3 | Listing page: filters and sort kept in the URL, pagination, product cards, skeleton loaders, empty state | Claude |
| 4 | Product page: photo gallery, variant picker (sold-out sizes disabled), price + MRP + % off, seller box, add to cart | Claude |
| 5 | Seller shop page | Claude |
| 6 | Cart backend: `carts`, `cart_items`; `GET/PUT/DELETE /api/cart/items`, `POST /api/cart/merge` (quantity capped by stock, max 10) | Claude |
| 7 | Cart frontend: guest cart in localStorage + Redux; cart page grouped by seller with the shipping rule; merge after login | Claude |
| 8 | Login, Register, Become-a-seller pages; route guards (copied from EventHub) | Claude |
| 9 | Addresses page | Claude |
| 10 | Vitest: cart maths, URL filters | Claude |
| 11 | Automated phone (375 px) + dark-mode scan of every page; fix what it finds | Claude |

## Output of this phase

- The shop website on real API data
- Cart that survives login
- Every page works on phone and in dark mode

## Done when

- A guest adds 3 items from 2 sellers, logs in, and the cart is kept
- Every page passes the phone + dark-mode check

## Explain it back

- Why are filters kept in the URL?
- What happens to the guest cart when you log in?
- Why does the server recalculate prices even though the cart shows them?

**Skills you practise:** React components, React Router, Redux Toolkit, forms, responsive Tailwind, localStorage

**Where your work goes:** `frontend\src\pages`, `...\components`, `...\store`; `backend\...\cart`
