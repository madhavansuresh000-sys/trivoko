# 2. Outcomes: what each person can do

![Who uses TriVoKo and what each person can do](roles.png)

## 2.1 A day on TriVoKo (the demo story)

This story is also the script for the demo video and the interview walk-through.

1. **Ravi (customer)** searches "iphnoe 15". The typo is fixed and iPhone 15 cases show up with filters for brand and price.
2. He adds a phone case from **Seller A (Chennai Mobiles)** and running shoes from **Seller B (Kovai Sports)** to one cart.
3. At checkout he picks his saved address, applies coupon `WELCOME10` and pays **once** with the Stripe test card 4242 4242 4242 4242.
4. Order **TV-1001** is split into **two packages**. Chennai Mobiles sees only the phone case; Kovai Sports sees only the shoes.
5. Each seller marks their package **Packed → Shipped**. Ravi sees two tracking timelines and gets an email at each step.
6. The shoes do not fit. Ravi asks for a **return** with a reason and a photo. Kovai Sports approves, marks it picked up, and Ravi receives a **partial refund** of only the shoes (minus their share of the coupon).
7. **Kavya (customer)** had the same shoes in her wishlist. When Kovai Sports lowers the price, she gets a **price-drop alert**.
8. At 12:00 PM the **admin** starts a **flash sale**: 100 headphones at ₹999. Thousands click at the same moment; exactly 100 win, the rest see "Sold out" instantly.
9. The admin opens **reports**: sales per day, top sellers, return rate.

## 2.2 Outcomes per role

### Customer

| Can do | Shown on page |
| Browse categories, see deals and flash sales on the home page | Home |
| Search with typo tolerance and autocomplete; filter by category, brand, price, rating, in stock; sort | Search / listing |
| See photos, choose size/colour, read verified reviews, see "lowest price in 30 days" | Product page |
| Add to cart as a guest; the cart is kept after login | Cart |
| Save addresses, apply a coupon, pay once with Stripe | Checkout |
| Track every package of an order; download a PDF invoice | My orders / order detail |
| Cancel before shipping; return within 7 days after delivery | Order detail |
| Wishlist products and get price-drop alerts | Wishlist, bell, email |
| Review a product only after buying it | Order detail / product page |

### Seller

| Can do | Shown on page |
| Apply to become a seller (shop name, city, GST number optional); wait for admin approval | Become a seller |
| Add and edit products with variants, photos (Cloudinary), price, MRP and stock | Seller → Products |
| See low-stock warnings | Seller dashboard |
| See only their own packages and move them Placed → Packed → Shipped (tracking number) | Seller → Orders |
| Approve or reject return requests (with a reason) | Seller → Returns |
| Join an admin flash sale with a sale price and a fixed quantity | Seller → Flash sales |
| See earnings: every sale adds 90% to the ledger, every refund takes it back | Seller → Earnings |
| See sales charts for their shop (7/30/90 days) | Seller → Analytics |

### Admin

| Can do | Shown on page |
| Approve or reject seller applications and new products | Admin → Approvals |
| Search users, sellers and orders; block a seller or user | Admin → Users / Orders |
| Create coupons (flat or %, minimum order, expiry, one use per customer) | Admin → Coupons |
| Schedule flash sales and pick which seller offers join | Admin → Flash sales |
| Decide escalated return disputes | Admin → Returns |
| Site-wide reports: sales per day, top products, top sellers, return rate | Admin → Reports |
| Audit log: who changed what and when | Admin → Activity |
