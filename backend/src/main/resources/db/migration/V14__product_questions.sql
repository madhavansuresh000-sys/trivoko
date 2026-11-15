-- TriVoKo V14: product questions and answers (after launch).
-- "Does this case fit the V12 5G?" - any logged-in shopper asks on the product page; the product's seller answers.
-- seller_id is copied from the product when asked, so the seller's "questions to answer" list needs no join.
CREATE TABLE product_questions (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT        NOT NULL,
    seller_id   BIGINT        NOT NULL,
    asker_id    BIGINT        NOT NULL,
    question    VARCHAR(300)  NOT NULL,
    answer      VARCHAR(1000) NULL,
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    answered_at DATETIME(6)   NULL,
    CONSTRAINT fk_questions_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_questions_seller FOREIGN KEY (seller_id) REFERENCES sellers (id),
    CONSTRAINT fk_questions_asker FOREIGN KEY (asker_id) REFERENCES users (id),
    -- the product page: newest first
    INDEX ix_questions_product (product_id, created_at),
    -- the seller's list: unanswered first
    INDEX ix_questions_seller (seller_id, answered_at)
);
