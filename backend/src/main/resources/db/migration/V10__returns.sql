-- TriVoKo V10: returns & refunds (Phase 8).
-- A customer returns ONE order line; the seller (or the admin, after an escalation) decides; after pickup the
-- money goes back as a Stripe PARTIAL refund. The status only moves through ReturnStatus.allowedNext().

CREATE TABLE return_requests (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_item_id  BIGINT        NOT NULL,
    package_id     BIGINT        NOT NULL,
    order_id       BIGINT        NOT NULL,
    customer_id    BIGINT        NOT NULL,
    seller_id      BIGINT        NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    reason         VARCHAR(20)   NOT NULL,                    -- DAMAGED, WRONG_ITEM, NOT_AS_DESCRIBED, SIZE_FIT, CHANGED_MIND, OTHER
    note           VARCHAR(300)  NOT NULL,
    photo_url      VARCHAR(500)  NULL,
    refund_amount  DECIMAL(12,2) NOT NULL,                    -- line total minus its coupon share, fixed when asked
    reject_reason  VARCHAR(300)  NULL,
    escalated_at   DATETIME(6)   NULL,
    version        BIGINT        NOT NULL DEFAULT 0,          -- two clicks at the same moment: one wins, one gets 409
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_returns_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT fk_returns_package FOREIGN KEY (package_id) REFERENCES packages (id),
    CONSTRAINT fk_returns_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_returns_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_returns_seller FOREIGN KEY (seller_id) REFERENCES sellers (id),
    -- ONE return per order line, ever
    CONSTRAINT uq_returns_item UNIQUE (order_item_id),
    CONSTRAINT ck_returns_amount CHECK (refund_amount >= 0),
    INDEX ix_returns_seller (seller_id, status),
    INDEX ix_returns_customer (customer_id, id),
    INDEX ix_returns_status_created (status, created_at)
);

-- the customer's return timeline: one line per move
CREATE TABLE return_events (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    return_id  BIGINT       NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    actor      VARCHAR(20)  NOT NULL,                         -- CUSTOMER, SELLER, ADMIN, SYSTEM
    note       VARCHAR(300) NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_return_events_return FOREIGN KEY (return_id) REFERENCES return_requests (id),
    INDEX ix_return_events_return (return_id, id)
);

-- Seller ledger: V7 allowed ONE line per (package, kind). Two items of one package can both be returned, so a
-- REFUND line now also names its return. MySQL treats every NULL as different in a UNIQUE key, so SALE lines
-- (return_id NULL) use once_key = 0 - "Mark delivered" twice still cannot pay twice.
ALTER TABLE seller_ledger ADD COLUMN return_id BIGINT NULL;
ALTER TABLE seller_ledger ADD COLUMN once_key BIGINT AS (COALESCE(return_id, 0)) STORED;
-- the new key first: package_id must keep an index for its foreign key while the old key is dropped
ALTER TABLE seller_ledger ADD CONSTRAINT uq_ledger_once_v10 UNIQUE (package_id, kind, once_key);
ALTER TABLE seller_ledger DROP INDEX uq_ledger_once;
