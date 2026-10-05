package com.trivoko.user.dto;

/** One saved address, as the "My addresses" page and the checkout show it. */
public record AddressView(Long id, String name, String phone, String line1, String line2, String city,
		String state, String pincode, boolean isDefault) {
}
