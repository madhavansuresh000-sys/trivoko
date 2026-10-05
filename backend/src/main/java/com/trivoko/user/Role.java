package com.trivoko.user;

/**
 * What a person may do. Everyone is a CUSTOMER; SELLER is added when the admin approves their shop;
 * ADMIN runs the mall office. One person can have several roles (stored in user_roles).
 */
public enum Role {
	CUSTOMER, SELLER, ADMIN
}
