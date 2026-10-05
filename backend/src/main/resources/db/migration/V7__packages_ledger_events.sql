-- TriVoKo V7: the seller back office (Phase 5) - package moves, their timeline, and the seller's account book.

ALTER TABLE packages
    ADD COLUMN tracking_number VARCHAR(30)  NULL,
    ADD COLUMN cancel_reason   VARCHAR(200) NULL,
    ADD COLUMN placed_at       DATETIME(6)  NULL,
    ADD COLUMN delivered_at    DATETIME(6)  NULL;

-- partial refunds (one cancelled package) add up here; status REFUNDED when it reaches the amount
ALTER TABLE payments
    ADD COLUMN refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0;

-- every move of a package = one line of the customer's tracking timeline
CREATE TABLE package_events (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT       NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    actor      VARCHAR(20)  NOT NULL,                        -- SYSTEM, SELLER, CUSTOMER
    note       VARCHAR(200),
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_package_events_package FOREIGN KEY (package_id) REFERENCES packages (id),
    INDEX ix_package_events_package (package_id, id)
);

-- the seller's account book: balance = SUM(amount), never one stored number (every rupee can be explained)
CREATE TABLE seller_ledger (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_id  BIGINT        NOT NULL,
    package_id BIGINT        NOT NULL,
    kind       VARCHAR(20)   NOT NULL,                       -- SALE (+90% on delivery); REFUND comes in Phase 8
    amount     DECIMAL(12,2) NOT NULL,
    note       VARCHAR(200),
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ledger_seller FOREIGN KEY (seller_id) REFERENCES sellers (id),
    CONSTRAINT fk_ledger_package FOREIGN KEY (package_id) REFERENCES packages (id),
    CONSTRAINT uq_ledger_once UNIQUE (package_id, kind),     -- a double click can never pay a sale twice
    INDEX ix_ledger_seller (seller_id, id)
);

-- packages of orders paid before V7 count as placed when they were paid
UPDATE packages p JOIN orders o ON o.id = p.order_id SET p.placed_at = o.paid_at WHERE p.status = 'PLACED';
