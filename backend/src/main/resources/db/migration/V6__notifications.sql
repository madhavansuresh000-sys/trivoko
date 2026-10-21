-- TriVoKo V6: the bell and its emails (Phase 4, copied from EventHub).
-- A row is shown under the bell; when email_status = PENDING it is also emailed after the commit, with retries.

CREATE TABLE notifications (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    kind           VARCHAR(30)   NOT NULL,                     -- ORDER_PLACED, NEW_PACKAGE ...
    title          VARCHAR(200)  NOT NULL,
    body           VARCHAR(1000) NOT NULL,
    link           VARCHAR(300),                               -- a page of the shop, e.g. /orders/TV-100123
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at        DATETIME(6),
    email_status   VARCHAR(10)   NOT NULL,                     -- NONE (bell only), PENDING, SENT, FAILED
    email_attempts INT           NOT NULL DEFAULT 0,
    emailed_at     DATETIME(6),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_notifications_user (user_id, created_at),
    INDEX ix_notifications_email (email_status, id)
);
