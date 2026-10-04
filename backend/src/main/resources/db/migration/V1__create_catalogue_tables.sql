-- TriVoKo V1: catalogue tables for Phase 1 (categories, sellers, products, variants, images, price history)
-- Status columns use VARCHAR + CHECK (not MySQL ENUM) so they map cleanly to Java enums (the EventHub rule).
-- Money is always DECIMAL(12,2) - never FLOAT/DOUBLE, which cannot store 0.10 exactly.

-- A category tree: "Mobiles & Accessories" (parent_id NULL) -> "Mobiles", "Cases & covers" ...
CREATE TABLE categories (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(80)  NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    parent_id  BIGINT,
    sort_order INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
);

-- A shop on TriVoKo, e.g. Chennai Mobiles. Phase 2 links it to the seller's user account.
CREATE TABLE sellers (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shop_name   VARCHAR(120) NOT NULL,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    city        VARCHAR(80)  NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_sellers_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'BLOCKED'))
);

-- A product page, e.g. "Volta V12 5G". Price and stock live on its variants.
-- price_from / mrp_from = the cheapest variant, kept up to date by ProductService (one place only),
-- so "filter by price" and "sort by price" are simple columns in one SQL query.
CREATE TABLE products (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_id        BIGINT        NOT NULL,
    category_id      BIGINT        NOT NULL,
    name             VARCHAR(200)  NOT NULL,
    slug             VARCHAR(220)  NOT NULL UNIQUE,
    brand            VARCHAR(80)   NOT NULL,
    description      TEXT,
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    rejection_reason VARCHAR(300),
    price_from       DECIMAL(12,2),
    mrp_from         DECIMAL(12,2),
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_products_seller   FOREIGN KEY (seller_id)   REFERENCES sellers (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_products_status CHECK (status IN ('DRAFT', 'PENDING', 'ACTIVE', 'REJECTED', 'BLOCKED'))
);
CREATE INDEX ix_products_listing ON products (status, category_id, price_from);
CREATE INDEX ix_products_brand   ON products (brand);

-- One buyable version of a product, e.g. "Black / 128 GB". Stock is counted here.
-- version = optimistic lock: two buyers saving the same row at once -> the second one gets 409.
CREATE TABLE product_variants (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT        NOT NULL,
    sku        VARCHAR(60)   NOT NULL UNIQUE,
    label      VARCHAR(100)  NOT NULL,
    size       VARCHAR(20),
    colour     VARCHAR(40),
    price      DECIMAL(12,2) NOT NULL,
    mrp        DECIMAL(12,2) NOT NULL,
    stock      INT           NOT NULL DEFAULT 0,
    version    BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_variants_price CHECK (price > 0),
    CONSTRAINT ck_variants_mrp   CHECK (mrp >= price),
    CONSTRAINT ck_variants_stock CHECK (stock >= 0)
);

-- Product photos (Cloudinary URLs). sort_order 0 = the main photo.
CREATE TABLE product_images (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL,
    url        VARCHAR(500) NOT NULL,
    alt_text   VARCHAR(200),
    sort_order INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- Every price change of a variant (written by ProductService.changePrice).
-- Phase 9 uses it for "lowest price in 30 days" and price-drop alerts.
CREATE TABLE price_history (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    variant_id BIGINT        NOT NULL,
    old_price  DECIMAL(12,2) NOT NULL,
    new_price  DECIMAL(12,2) NOT NULL,
    changed_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_price_history_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
);
CREATE INDEX ix_price_history_variant ON price_history (variant_id, changed_at);
