-- TriVoKo V3: accounts and roles for Phase 2 (users, roles, addresses, audit log; every shop gets an owner)

-- A person who can log in. Passwords are stored only as BCrypt hashes, never as plain text.
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(10),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,   -- FALSE = blocked by the admin
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

-- One row per role, so one person can be CUSTOMER and SELLER at the same time.
CREATE TABLE user_roles (
    user_id BIGINT      NOT NULL,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('CUSTOMER', 'SELLER', 'ADMIN'))
);

-- Delivery addresses: max 5 per user, exactly one default (both rules live in AddressService).
CREATE TABLE addresses (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(10)  NOT NULL,
    line1      VARCHAR(160) NOT NULL,
    line2      VARCHAR(160),
    city       VARCHAR(80)  NOT NULL,
    state      VARCHAR(80)  NOT NULL,
    pincode    CHAR(6)      NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_addresses_user (user_id)
);

-- Who changed what and when (approvals, rejections, blocks, price changes).
CREATE TABLE audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT,                               -- NULL = done by the system
    action        VARCHAR(40)  NOT NULL,
    entity_type   VARCHAR(30)  NOT NULL,
    entity_id     BIGINT       NOT NULL,
    details       VARCHAR(500),
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES users (id),
    INDEX ix_audit_log_entity (entity_type, entity_id)
);

-- ---------- Demo accounts and shop owners ----------
-- The password hash below can never match a real password. In the dev profile DevDataSeeder
-- replaces it with BCrypt(DEMO_PASSWORD from .env), so the real password is never in Git.
INSERT INTO users (id, email, password_hash, full_name, phone) VALUES
  (1,  'admin@trivoko.test',             '{noop}!locked', 'TriVoKo Admin',      '9000000001'),
  (2,  'ravi@trivoko.test',              '{noop}!locked', 'Ravi Kumar',         '9000000002'),
  (3,  'kavya@trivoko.test',             '{noop}!locked', 'Kavya Iyer',         '9000000003'),
  (4,  'chennai.mobiles@trivoko.test',   '{noop}!locked', 'Arun Prakash',       '9000000004'),
  (5,  'kovai.sports@trivoko.test',      '{noop}!locked', 'Senthil Murugan',    '9000000005'),
  (6,  'bengaluru.gadgets@trivoko.test', '{noop}!locked', 'Nikhil Rao',         '9000000006'),
  (7,  'madurai.handlooms@trivoko.test', '{noop}!locked', 'Meenakshi Sundaram', '9000000007'),
  (8,  'mumbai.style@trivoko.test',      '{noop}!locked', 'Rohan Mehta',        '9000000008'),
  (9,  'salem.steel@trivoko.test',       '{noop}!locked', 'Lakshmi Narayanan',  '9000000009'),
  (10, 'pondy.books@trivoko.test',       '{noop}!locked', 'Claire Joseph',      '9000000010'),
  (11, 'tirupur.kids@trivoko.test',      '{noop}!locked', 'Divya Ramesh',       '9000000011'),
  (12, 'erode.organics@trivoko.test',    '{noop}!locked', 'Gowtham Selvan',     '9000000012');

-- everyone is a customer
INSERT INTO user_roles (user_id, role)
SELECT id, 'CUSTOMER' FROM users;

INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');

-- owners of the 8 APPROVED shops (sellers 1-8 -> users 4-11) are sellers; Erode Organics (PENDING) is not yet
INSERT INTO user_roles (user_id, role)
SELECT id, 'SELLER' FROM users WHERE id BETWEEN 4 AND 11;

-- ---------- Every shop gets an owner ----------
-- Safe order: add the column empty, fill it, then make it required.
ALTER TABLE sellers
    ADD COLUMN user_id       BIGINT       NULL AFTER id,
    ADD COLUMN gstin         VARCHAR(15)  NULL AFTER city,
    ADD COLUMN reject_reason VARCHAR(300) NULL AFTER status;

UPDATE sellers SET user_id = id + 3;

ALTER TABLE sellers
    MODIFY COLUMN user_id BIGINT NOT NULL,
    ADD CONSTRAINT uq_sellers_user UNIQUE (user_id),
    ADD CONSTRAINT fk_sellers_user FOREIGN KEY (user_id) REFERENCES users (id);
