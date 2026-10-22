package com.trivoko.notify;

/**
 * ORDER_PLACED to the customer, NEW_PACKAGE to each seller; PACKAGE_* = every later move of a package (Phase 5);
 * SHOP_* and PRODUCT_* = the admin's decisions, sent to the shop owner (Phase 6).
 */
public enum NotificationKind {
	ORDER_PLACED, NEW_PACKAGE, PACKAGE_PACKED, PACKAGE_SHIPPED, PACKAGE_DELIVERED, PACKAGE_CANCELLED,
	SHOP_APPROVED, SHOP_REJECTED, SHOP_BLOCKED, SHOP_UNBLOCKED, PRODUCT_APPROVED, PRODUCT_REJECTED
}
