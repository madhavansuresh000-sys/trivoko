package com.trivoko.catalog.dto;

/** One variant that is running out: "Kovai Run Pro - UK 8: only 3 left". */
public record LowStockView(Long productId, String productName, Long variantId, String variantLabel, int stock) {
}
