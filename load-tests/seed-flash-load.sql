-- Seed for the flash-sale load test (run against the throw-away database trivoko_load, NEVER the dev one).
-- 5,000 customers, one live sale, one deal with 100 units.
SET SESSION cte_max_recursion_depth = 10000;

INSERT INTO users (email, password_hash, full_name)
WITH RECURSIVE n (i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 5000)
SELECT CONCAT('load', i, '@example.com'), '{noop}!locked', 'Load Customer' FROM n;

INSERT INTO user_roles (user_id, role)
SELECT id, 'CUSTOMER' FROM users WHERE email LIKE 'load%@example.com';

INSERT INTO flash_sales (title, starts_at, ends_at)
VALUES ('Load test sale', NOW() - INTERVAL 5 MINUTE, NOW() + INTERVAL 2 HOUR);

INSERT INTO flash_sale_items (sale_id, variant_id, seller_id, sale_price, quantity)
SELECT (SELECT MAX(id) FROM flash_sales), v.id, p.seller_id, 99.00, 100
FROM product_variants v JOIN products p ON p.id = v.product_id
WHERE p.status = 'ACTIVE' ORDER BY v.id LIMIT 1;
