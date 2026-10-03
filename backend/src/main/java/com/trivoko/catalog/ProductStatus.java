package com.trivoko.catalog;

/**
 * A product's life: the seller writes it (DRAFT), sends it for checking (PENDING),
 * the admin approves it (ACTIVE = visible in the shop) or rejects it (REJECTED, with a reason).
 * The admin can also hide a live product (BLOCKED). Only ACTIVE products are public.
 */
public enum ProductStatus {
	DRAFT, PENDING, ACTIVE, REJECTED, BLOCKED
}
