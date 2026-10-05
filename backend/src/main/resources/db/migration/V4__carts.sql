-- TriVoKo V4: the saved cart of a logged-in customer (Phase 3). A guest's cart lives in the browser
-- and is merged into this one after login. No prices here: they are always read fresh from the catalogue.

CREATE TABLE carts (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL UNIQUE,                 -- one cart per user
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE cart_items (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cart_id    BIGINT      NOT NULL,
    variant_id BIGINT      NOT NULL,
    quantity   INT         NOT NULL,
    added_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT uq_cart_items_line UNIQUE (cart_id, variant_id),   -- the same size twice = one line, quantity 2
    CONSTRAINT ck_cart_items_quantity CHECK (quantity BETWEEN 1 AND 10)
);
