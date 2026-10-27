package com.trivoko.catalog.dto;

/**
 * A product someone saved, as the wishlist (and the price-drop job) need it. live = ACTIVE product of an APPROVED
 * shop; a saved product that was blocked later is still shown, marked "Not available".
 */
public record WatchedProduct(ProductCard card, boolean live) {
}
