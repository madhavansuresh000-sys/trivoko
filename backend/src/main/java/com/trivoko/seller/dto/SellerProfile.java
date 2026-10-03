package com.trivoko.seller.dto;

import java.time.LocalDate;

/** The top of a shop page. Its products come from GET /api/products?seller={slug}. */
public record SellerProfile(String shopName, String slug, String city, String description, LocalDate sellingSince) {
}
