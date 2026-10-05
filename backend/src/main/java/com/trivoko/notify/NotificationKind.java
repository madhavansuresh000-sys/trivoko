package com.trivoko.notify;

/** ORDER_PLACED to the customer, NEW_PACKAGE to each seller; PACKAGE_* = every later move of a package (Phase 5). */
public enum NotificationKind {
	ORDER_PLACED, NEW_PACKAGE, PACKAGE_PACKED, PACKAGE_SHIPPED, PACKAGE_DELIVERED, PACKAGE_CANCELLED
}
