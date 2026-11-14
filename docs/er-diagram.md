# TriVoKo database - ER diagram

30 tables, all created by Flyway (`backend/src/main/resources/db/migration`, V1-V14). GitHub draws the diagram
below from the text. Read a line like `ORDERS ||--|{ PACKAGES : "split into"` as: **one** order has **one or more**
packages. Every table belongs to exactly one module (see each module's `package-info.java`); another module never
reads it directly - it calls that module's service (checked by ArchUnit).

```mermaid
erDiagram
    %% catalog
    CATEGORIES ||--o{ CATEGORIES : "parent of"
    CATEGORIES ||--o{ PRODUCTS : contains
    SELLERS ||--o{ PRODUCTS : sells
    PRODUCTS ||--|{ PRODUCT_VARIANTS : "comes in"
    PRODUCTS ||--o{ PRODUCT_IMAGES : "shown by"
    PRODUCT_VARIANTS ||--o{ PRICE_HISTORY : "price changes"

    %% user / auth / admin
    USERS ||--|{ USER_ROLES : has
    USERS ||--o{ ADDRESSES : "ships to"
    USERS |o--o| SELLERS : "owns shop"
    USERS ||--o{ AUDIT_LOG : "admin actions"
    USERS ||--o{ NOTIFICATIONS : "bell + e-mail"

    %% cart
    USERS ||--o| CARTS : keeps
    CARTS ||--o{ CART_ITEMS : holds
    PRODUCT_VARIANTS ||--o{ CART_ITEMS : "added as"

    %% order / payment / coupon
    USERS ||--o{ ORDERS : places
    ORDERS ||--|{ PACKAGES : "split into (one per seller)"
    SELLERS ||--o{ PACKAGES : ships
    PACKAGES ||--|{ ORDER_ITEMS : contains
    PRODUCT_VARIANTS ||--o{ ORDER_ITEMS : "bought as"
    ORDERS ||--o{ PAYMENTS : "paid by"
    PACKAGES ||--o{ PACKAGE_EVENTS : "tracking timeline"
    SELLERS ||--o{ SELLER_LEDGER : "earns 90%"
    PACKAGES ||--o{ SELLER_LEDGER : "credits / debits"
    COUPONS ||--o{ COUPON_REDEMPTIONS : "used in"
    ORDERS ||--o| COUPON_REDEMPTIONS : uses
    USERS ||--o{ COUPON_REDEMPTIONS : redeems

    %% flash sale
    FLASH_SALES ||--|{ FLASH_SALE_ITEMS : offers
    PRODUCT_VARIANTS ||--o{ FLASH_SALE_ITEMS : "on sale as"
    SELLERS ||--o{ FLASH_SALE_ITEMS : provides
    FLASH_SALE_ITEMS ||--o{ FLASH_SALE_CLAIMS : "won by"
    USERS ||--o{ FLASH_SALE_CLAIMS : claims

    %% returns
    ORDER_ITEMS ||--o{ RETURN_REQUESTS : "returned via"
    PACKAGES ||--o{ RETURN_REQUESTS : "from package"
    USERS ||--o{ RETURN_REQUESTS : asks
    SELLERS ||--o{ RETURN_REQUESTS : decides
    RETURN_REQUESTS ||--|{ RETURN_EVENTS : "state history"

    %% reviews, wishlist, questions
    PRODUCTS ||--o{ REVIEWS : "rated in"
    USERS ||--o{ REVIEWS : writes
    USERS ||--o{ WISHLIST_ITEMS : saves
    PRODUCTS ||--o{ WISHLIST_ITEMS : "saved as"
    PRODUCTS ||--o{ PRODUCT_QUESTIONS : "asked about"
    USERS ||--o{ PRODUCT_QUESTIONS : asks
    SELLERS ||--o{ PRODUCT_QUESTIONS : answers
```

Tables without arrows: `processed_payment_events` (remembers each Stripe webhook id so a repeated webhook is
ignored - "idempotency"). Lucene's search index is not a table: it lives in a folder and is rebuilt from MySQL on start.

## Three ideas worth explaining in an interview

1. **One order, many packages.** `orders` is what the customer paid once; `packages` is what each seller ships.
   Returns, tracking and seller money all hang off the package, not the order.
2. **Money is a ledger, never a single number.** `seller_ledger` rows are added (sale +90%, refund -90%), never
   edited - like a bank passbook. A seller's balance is the SUM of their rows.
3. **History tables instead of overwriting.** `price_history`, `package_events`, `return_events`, `audit_log` keep
   every change with a time, so "lowest price in 30 days", the tracking timeline and "who approved this?" are just
   queries.
