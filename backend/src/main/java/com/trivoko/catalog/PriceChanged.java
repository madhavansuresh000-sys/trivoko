package com.trivoko.catalog;

/**
 * "A price of this product changed" - published by ProductService.changePrice. The wishlist module listens
 * (after the commit) and only marks its rows; the price-drop job decides later whether to tell anyone.
 */
public record PriceChanged(Long productId) {
}
