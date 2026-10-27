-- TriVoKo V12: reviews, wishlist and price-drop alerts (Phase 9).
-- price_history already has ix_price_history_variant (variant_id, changed_at) from V1 - the 30-day question uses it.

-- A verified-buyer review: ONE per customer per product; the author may edit or delete it.
CREATE TABLE reviews (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT        NOT NULL,
    user_id    BIGINT        NOT NULL,
    rating     INT           NOT NULL,
    title      VARCHAR(80)   NULL,
    body       VARCHAR(1000) NOT NULL,
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_reviews_once UNIQUE (product_id, user_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    -- the product page: newest first
    INDEX ix_reviews_product_created (product_id, created_at)
);

-- A saved product. price_when_added / last_alerted_price are the product's "from" price at that moment.
-- price_changed is set by the PriceChanged listener and cleared by PriceAlertJob, so the job reads only these rows.
CREATE TABLE wishlist_items (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT        NOT NULL,
    product_id         BIGINT        NOT NULL,
    price_when_added   DECIMAL(12,2) NOT NULL,
    last_alerted_price DECIMAL(12,2) NULL,
    price_changed      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_wishlist_once UNIQUE (user_id, product_id),
    INDEX ix_wishlist_product (product_id),
    INDEX ix_wishlist_changed (price_changed)
);

-- Kept on the product so a listing of 20 cards needs no extra query for stars.
ALTER TABLE products ADD COLUMN rating_avg DECIMAL(2,1) NULL;
ALTER TABLE products ADD COLUMN rating_count INT NOT NULL DEFAULT 0;
