-- TriVoKo V9: flash sales (Phase 7).
-- The sale quantity is TAKEN OUT of normal stock when a seller offers it, so a flash unit can never be sold twice.
-- "sold" only goes up through ONE atomic UPDATE ... WHERE sold < quantity (FlashItemRepository.claim).

CREATE TABLE flash_sales (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(120) NOT NULL,
    starts_at  DATETIME(6)  NOT NULL,
    ends_at    DATETIME(6)  NOT NULL,
    settled    BOOLEAN      NOT NULL DEFAULT FALSE,          -- unsold units already went back to normal stock
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_flash_sales_window CHECK (ends_at > starts_at),
    INDEX ix_flash_sales_window (starts_at, ends_at)
);

CREATE TABLE flash_sale_items (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sale_id    BIGINT        NOT NULL,
    variant_id BIGINT        NOT NULL,
    seller_id  BIGINT        NOT NULL,
    sale_price DECIMAL(12,2) NOT NULL,
    quantity   INT           NOT NULL,
    sold       INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_flash_items_sale FOREIGN KEY (sale_id) REFERENCES flash_sales (id),
    CONSTRAINT fk_flash_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_flash_items_seller FOREIGN KEY (seller_id) REFERENCES sellers (id),
    CONSTRAINT uq_flash_items_sale_variant UNIQUE (sale_id, variant_id),
    -- the database itself refuses an oversell, even if the code were wrong
    CONSTRAINT ck_flash_items_qty CHECK (quantity > 0 AND sold >= 0 AND sold <= quantity),
    CONSTRAINT ck_flash_items_price CHECK (sale_price > 0)
);

CREATE TABLE flash_sale_claims (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    status     VARCHAR(10) NOT NULL,                          -- HELD (10 minutes), ORDERED (in an unpaid order), PAID
    expires_at DATETIME(6) NOT NULL,
    order_id   BIGINT      NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_flash_claims_item FOREIGN KEY (item_id) REFERENCES flash_sale_items (id),
    CONSTRAINT fk_flash_claims_user FOREIGN KEY (user_id) REFERENCES users (id),
    -- ONE unit per customer per deal: a second claim hits this wall
    CONSTRAINT uq_flash_claims_item_user UNIQUE (item_id, user_id),
    INDEX ix_flash_claims_status_expiry (status, expires_at),
    INDEX ix_flash_claims_user (user_id, status)
);

-- an order line bought at the flash price remembers which deal it came from
ALTER TABLE order_items ADD COLUMN flash_item_id BIGINT NULL;
