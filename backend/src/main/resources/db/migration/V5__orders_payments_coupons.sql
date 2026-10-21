-- TriVoKo V5: orders split into packages, payments and coupons (Phase 4).
-- An order item COPIES the product name, variant and price, so a later price change never changes an old bill.

CREATE TABLE coupons (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(30)   NOT NULL UNIQUE,                 -- always upper case
    description  VARCHAR(200)  NOT NULL,
    type         VARCHAR(10)   NOT NULL,                        -- FLAT (rupees off) or PERCENT
    value        DECIMAL(12,2) NOT NULL,
    max_discount DECIMAL(12,2),                                 -- PERCENT only: the most it can take off
    min_order    DECIMAL(12,2) NOT NULL DEFAULT 0,             -- items total needed to use it
    valid_from   DATETIME(6)   NOT NULL,
    valid_until  DATETIME(6)   NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_coupons_type CHECK (type IN ('FLAT', 'PERCENT')),
    CONSTRAINT ck_coupons_value CHECK (value > 0)
);

CREATE TABLE orders (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    number          VARCHAR(12)   NOT NULL UNIQUE,              -- e.g. TV-100123 (shown to people)
    user_id         BIGINT        NOT NULL,
    status          VARCHAR(20)   NOT NULL,                     -- PENDING_PAYMENT, PAID, EXPIRED
    items_total     DECIMAL(12,2) NOT NULL,
    discount_total  DECIMAL(12,2) NOT NULL,
    shipping_total  DECIMAL(12,2) NOT NULL,
    grand_total     DECIMAL(12,2) NOT NULL,
    coupon_code     VARCHAR(30),
    -- the delivery address is COPIED: editing the address book later does not move a parcel
    ship_name       VARCHAR(100)  NOT NULL,
    ship_phone      VARCHAR(10)   NOT NULL,
    ship_line1      VARCHAR(160)  NOT NULL,
    ship_line2      VARCHAR(160),
    ship_city       VARCHAR(80)   NOT NULL,
    ship_state      VARCHAR(80)   NOT NULL,
    ship_pincode    CHAR(6)       NOT NULL,
    hold_expires_at DATETIME(6)   NOT NULL,
    paid_at         DATETIME(6),
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_orders_user (user_id, created_at),
    INDEX ix_orders_status_hold (status, hold_expires_at)        -- the expiry job's question
);

-- one package per seller: each shop packs and ships its own box
CREATE TABLE packages (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT        NOT NULL,
    seller_id    BIGINT        NOT NULL,
    seller_name  VARCHAR(120)  NOT NULL,
    status       VARCHAR(20)   NOT NULL,                        -- PENDING_PAYMENT, PLACED, EXPIRED (more in Phase 5)
    items_total  DECIMAL(12,2) NOT NULL,
    discount     DECIMAL(12,2) NOT NULL,
    shipping_fee DECIMAL(12,2) NOT NULL,
    total        DECIMAL(12,2) NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_packages_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_packages_seller FOREIGN KEY (seller_id) REFERENCES sellers (id),
    INDEX ix_packages_seller (seller_id, status)
);

CREATE TABLE order_items (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    package_id     BIGINT        NOT NULL,
    variant_id     BIGINT        NOT NULL,
    product_slug   VARCHAR(220)  NOT NULL,
    product_name   VARCHAR(200)  NOT NULL,
    variant_label  VARCHAR(100)  NOT NULL,
    unit_price     DECIMAL(12,2) NOT NULL,
    mrp            DECIMAL(12,2) NOT NULL,
    quantity       INT           NOT NULL,
    line_total     DECIMAL(12,2) NOT NULL,
    discount_share DECIMAL(12,2) NOT NULL,                      -- this item's part of the coupon (exact refunds later)
    CONSTRAINT fk_order_items_package FOREIGN KEY (package_id) REFERENCES packages (id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity BETWEEN 1 AND 10)
);

CREATE TABLE payments (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT        NOT NULL,
    provider   VARCHAR(10)   NOT NULL,                          -- FAKE (dev page) or STRIPE
    session_id VARCHAR(255)  NOT NULL UNIQUE,
    amount     DECIMAL(12,2) NOT NULL,
    status     VARCHAR(10)   NOT NULL,                          -- CREATED, PAID, REFUNDED, EXPIRED
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    paid_at    DATETIME(6),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- a webhook can arrive twice: the second time its event id is already here, so nothing happens
CREATE TABLE processed_payment_events (
    event_id     VARCHAR(255) NOT NULL PRIMARY KEY,
    processed_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE coupon_redemptions (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coupon_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    order_id    BIGINT      NOT NULL,
    redeemed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_redemptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_redemptions_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT uq_redemptions_once UNIQUE (coupon_id, user_id)  -- one use per customer
);

INSERT INTO coupons (code, description, type, value, max_discount, min_order, valid_from, valid_until) VALUES
  ('WELCOME10', '10% off your order (up to ₹500) on orders from ₹499', 'PERCENT', 10.00, 500.00, 499.00,
   NOW(6) - INTERVAL 1 DAY, NOW(6) + INTERVAL 365 DAY),
  ('FLAT100', '₹100 off on orders from ₹999', 'FLAT', 100.00, NULL, 999.00,
   NOW(6) - INTERVAL 1 DAY, NOW(6) + INTERVAL 365 DAY);
