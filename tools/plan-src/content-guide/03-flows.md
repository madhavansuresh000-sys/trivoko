# 3. The important flows, step by step

## 3.1 Buying from two sellers with one payment

![The order split](order-split.png)

1. The cart is priced **on the server** every time (`CartPricing`) - the browser's prices are never trusted.
2. **Checkout preview** shows one box per seller (= one package), the coupon and delivery: free from Rs 499 per package, else Rs 40.
3. **Pay** (`POST /api/orders`): the server checks the prices again (409 if they changed), **holds the stock** with one atomic `UPDATE ... WHERE stock >= qty` per item, and creates one order with one package per seller.
4. The **coupon is shared across the items by price**; the last item takes the rounding remainder, so the shares always add up exactly. Each item stores its share - this makes later refunds exact.
5. The customer pays on **Stripe** (test mode) or the built-in test payment page. Stripe tells the server by a **webhook** (signature checked; a duplicate message changes nothing).
6. Paid: order PAID, packages PLACED, bell + email + PDF invoice. Not paid in 10 minutes: EXPIRED, stock goes back.

>> One bill at the supermarket, but the bags are packed at different counters - each counter (seller) packs and sends its own bag.

## 3.2 A seller ships a package

```
 PLACED --pack--> PACKED --tracking no.--> SHIPPED --> DELIVERED
    |                                                    (+90% to the seller's ledger)
    +--- before shipping ---> CANCELLED (package refunded, stock back)
```

Every move goes through one method that knows the allowed next states; an illegal move is 409. If two people change the same package at once, `@Version` (optimistic locking) makes one of them get 409 instead of both "winning".

## 3.3 The flash sale

![Flash sale](flash-sale.png)

The whole claim is **one SQL statement**:

```sql
UPDATE flash_sale_items SET sold = sold + 1
 WHERE id = ? AND sold < quantity AND <the sale is live>
```

MySQL locks the row, checks and adds in one step. 1 row changed = you won; 0 = sold out. Three more guards: `UNIQUE (item_id, user_id)` = one per person; `CHECK (sold <= quantity)` = the database itself refuses an oversell; a job gives back units not paid within 10 minutes. A rate limit (5 claims per user, 200 per address, every 10 s) answers 429.

**Proof:** k6 with 5,000 users pressing at the same instant: exactly 100 winners, 4,900 "sold out", 0 errors, 0 oversell.

## 3.4 A return and its partial refund

![Return states](states.png)

```
 REQUESTED -> APPROVED -> PICKED_UP -> REFUNDED
     |            (seller silent 2 days = approved automatically)
     +-> REJECTED -> ESCALATED (once) -> admin decides
```

**Refund = item total - that item's coupon share** (delivery is not refunded). Stripe is called with the idempotency key `refund-return-<id>`, so ten clicks still refund once. In the same transaction: stock +1, the seller's ledger gets **-90%** of the refund, the customer gets a bell + email.

## 3.5 Price-drop alerts

![Price alerts and search](price-search.png)

A price change writes a `price_history` row and an event. After the save is committed, the event only **marks** the wishlist rows of that product. Every 10 minutes a job looks at marked rows and sends **one** alert if today's price is at least 1% below the last price the customer heard from us. Five price edits in a minute = one alert.

## 3.6 Smart search

Lucene (through Hibernate Search) finds and ranks up to 500 product ids, allowing typos by word length (1 typo from 5 letters, 2 from 8, first letter must be right). MySQL then applies the live filters - in stock, category, brand, price - and counts the facets. Lucene for the text, MySQL for the facts that change every minute.

## 3.7 One-click demo logins (launch)

On the live site the login page offers "Try it as Ravi" (customer) and "Try it as Chennai Mobiles" (seller). It is the same HttpOnly cookie as a normal login. It is never offered for the admin, it is switched off unless `DEMO_ONE_CLICK=true`, and a blocked demo account stays blocked.
