-- TriVoKo V11 (Phase 8 step 9): a return remembers WHAT was returned - a copy of the bill line, like order_items
-- itself - so the return pages show "Kovai Run Pro (UK 8) x1, order TV-100123, Kovai Sports" without asking
-- the order module once per row.
ALTER TABLE return_requests
    ADD COLUMN order_number  VARCHAR(12)  NOT NULL DEFAULT '',
    ADD COLUMN product_name  VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN variant_label VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN quantity      INT          NOT NULL DEFAULT 1,
    ADD COLUMN seller_name   VARCHAR(120) NOT NULL DEFAULT '';
