-- After launch (decision D13): the live demo is shared by every visitor, who all buy from the same sample shops.
-- demo_stock = each sample variant's stock when the shop opened. On the demo server (DEMO_ONE_CLICK=true) a night
-- job puts stock that visitors bought back up to this level, so the demo shop never runs empty.
-- Sizes that were sold out on purpose (stock 0) stay sold out; products added later (NULL) are never touched.
ALTER TABLE product_variants ADD COLUMN demo_stock INT NULL;
UPDATE product_variants SET demo_stock = stock;
